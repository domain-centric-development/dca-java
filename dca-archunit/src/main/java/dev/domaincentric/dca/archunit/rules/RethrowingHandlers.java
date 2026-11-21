package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.Source;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/**
 * Whether the catch handlers of a type around a call only rethrow the failure they caught.
 *
 * <p>ArchUnit's import model carries a try block's caught types but neither its {@code throw} nor
 * its {@code return}, so a handler that cleans up and rethrows looks the same as one that swallows.
 * This reads the method's byte code from the class file. A handler <em>rethrows unchanged</em> when
 * every path from its entry, following jumps and switches, ends in an {@code athrow} of the very
 * object the handler received - kept on the stack, or stored in a local variable and loaded again -
 * and no path reaches a {@code return}. A path that leaves the handler and runs on after the try
 * statement ends in the method's {@code return} or another {@code throw} and fails the test, as
 * does {@code throw new X(e)}. The edges a failure inside the clean-up would take are not followed:
 * the clean-up failing is not the handler's answer. Only the local variables and the top of the
 * operand stack are tracked; a rethrow whose object passes through a field, an array or a method
 * call is not recognised and counts as an answer. A class file that cannot be read, or a handler
 * the reader cannot place, counts as an answer as well - the rule stays conservative.
 */
final class RethrowingHandlers {

  private static final int MAX_STATES = 20_000;

  private final TreeMap<String, Optional<ClassFile>> files = new TreeMap<>();

  /**
   * True when every handler of {@code caught} whose try range holds {@code call} rethrows the
   * caught object unchanged, and at least one such handler was found in the class file.
   */
  boolean rethrowsUnchanged(JavaCodeUnit unit, JavaAccess<?> call, JavaClass caught) {
    Optional<ClassFile> file =
        files.computeIfAbsent(unit.getOwner().getName(), name -> read(unit.getOwner()));
    if (file.isEmpty()) {
      return false;
    }
    String catchType = caught.getName().replace('.', '/');
    String owner = call.getTargetOwner().getName().replace('.', '/');
    String name = call.getTarget().getName();
    int line = call.getLineNumber();
    List<Method> named = new ArrayList<>();
    for (Method method : file.get().methods) {
      if (method.name.equals(unit.getName())) {
        named.add(method);
      }
    }
    // A lambda's body or another synthetic method holds the try when the unit's own one does not.
    Optional<Boolean> verdict = verdict(named, catchType, owner, name, line);
    if (verdict.isEmpty()) {
      verdict = verdict(file.get().methods, catchType, owner, name, line);
    }
    return verdict.orElse(false);
  }

  private static Optional<Boolean> verdict(
      List<Method> methods, String catchType, String owner, String name, int line) {
    boolean found = false;
    for (Method method : methods) {
      for (Handler handler : method.handlers) {
        if (!catchType.equals(handler.catchType)
            || !method.callsWithin(handler.start, handler.end, owner, name, line)) {
          continue;
        }
        found = true;
        if (!method.onlyRethrows(handler.pc)) {
          return Optional.of(false);
        }
      }
    }
    return found ? Optional.of(true) : Optional.empty();
  }

  private static Optional<ClassFile> read(JavaClass type) {
    Optional<Source> source = type.getSource();
    if (source.isEmpty()) {
      return Optional.empty();
    }
    try (InputStream in = source.get().getUri().toURL().openStream()) {
      return Optional.of(ClassFile.parse(in.readAllBytes()));
    } catch (IOException | RuntimeException unreadable) {
      return Optional.empty();
    }
  }

  private record Handler(int start, int end, int pc, String catchType) {}

  private record Ref(String owner, String name) {}

  private record ClassFile(List<Method> methods) {

    static ClassFile parse(byte[] bytes) throws IOException {
      DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
      if (in.readInt() != 0xCAFEBABE) {
        throw new IOException("not a class file");
      }
      in.readUnsignedShort();
      in.readUnsignedShort();
      int count = in.readUnsignedShort();
      String[] utf8 = new String[count];
      int[] first = new int[count];
      int[] second = new int[count];
      int[] tags = new int[count];
      for (int index = 1; index < count; index++) {
        int tag = in.readUnsignedByte();
        tags[index] = tag;
        switch (tag) {
          case 1 -> utf8[index] = in.readUTF();
          case 7, 8, 16, 19, 20 -> first[index] = in.readUnsignedShort();
          case 15 -> in.skipNBytes(3);
          case 3, 4 -> in.skipNBytes(4);
          case 9, 10, 11, 12, 17, 18 -> {
            first[index] = in.readUnsignedShort();
            second[index] = in.readUnsignedShort();
          }
          case 5, 6 -> {
            in.skipNBytes(8);
            index++;
          }
          default -> throw new IOException("unknown constant pool tag " + tag);
        }
      }
      Pool pool = new Pool(utf8, first, second, tags);
      in.skipNBytes(6);
      in.skipNBytes(2L * in.readUnsignedShort());
      int fields = in.readUnsignedShort();
      for (int field = 0; field < fields; field++) {
        in.skipNBytes(6);
        skipAttributes(in);
      }
      List<Method> methods = new ArrayList<>();
      int methodCount = in.readUnsignedShort();
      for (int m = 0; m < methodCount; m++) {
        in.readUnsignedShort();
        String name = utf8[in.readUnsignedShort()];
        in.readUnsignedShort();
        int attributes = in.readUnsignedShort();
        for (int a = 0; a < attributes; a++) {
          String attribute = utf8[in.readUnsignedShort()];
          int length = in.readInt();
          if ("Code".equals(attribute)) {
            methods.add(Method.parse(name, in, pool));
          } else {
            in.skipNBytes(length);
          }
        }
      }
      return new ClassFile(methods);
    }

    private static void skipAttributes(DataInputStream in) throws IOException {
      int attributes = in.readUnsignedShort();
      for (int a = 0; a < attributes; a++) {
        in.readUnsignedShort();
        in.skipNBytes(in.readInt() & 0xFFFFFFFFL);
      }
    }
  }

  private record Pool(String[] utf8, int[] first, int[] second, int[] tags) {

    String className(int index) {
      return index == 0 ? null : utf8[first[index]];
    }

    Ref member(int index) {
      if (tags[index] < 9 || tags[index] > 11) {
        return null;
      }
      return new Ref(className(first[index]), utf8[first[second[index]]]);
    }
  }

  private record Method(
      String name,
      byte[] code,
      List<Handler> handlers,
      TreeMap<Integer, Integer> lines,
      Pool pool) {

    static Method parse(String name, DataInputStream in, Pool pool) throws IOException {
      in.skipNBytes(4);
      byte[] code = new byte[in.readInt()];
      in.readFully(code);
      List<Handler> handlers = new ArrayList<>();
      int entries = in.readUnsignedShort();
      for (int e = 0; e < entries; e++) {
        int start = in.readUnsignedShort();
        int end = in.readUnsignedShort();
        int pc = in.readUnsignedShort();
        handlers.add(new Handler(start, end, pc, pool.className(in.readUnsignedShort())));
      }
      TreeMap<Integer, Integer> lines = new TreeMap<>();
      int attributes = in.readUnsignedShort();
      for (int a = 0; a < attributes; a++) {
        String attribute = pool.utf8()[in.readUnsignedShort()];
        int length = in.readInt();
        if ("LineNumberTable".equals(attribute)) {
          int count = in.readUnsignedShort();
          for (int l = 0; l < count; l++) {
            lines.put(in.readUnsignedShort(), in.readUnsignedShort());
          }
        } else {
          in.skipNBytes(length);
        }
      }
      return new Method(name, code, handlers, lines, pool);
    }

    /** Whether an invocation of {@code owner.name} on {@code line} lies in [start, end). */
    boolean callsWithin(int start, int end, String owner, String name, int line) {
      for (int pc = 0; pc < code.length && pc < end; pc += length(pc)) {
        int op = u1(pc);
        if (pc < start || op < 0xb6 || op > 0xb9) {
          continue;
        }
        Ref ref = pool.member(u2(pc + 1));
        if (ref == null || !owner.equals(ref.owner()) || !name.equals(ref.name())) {
          continue;
        }
        var at = lines.floorEntry(pc);
        if (at == null || line <= 0 || at.getValue() == line) {
          return true;
        }
      }
      return false;
    }

    /**
     * Whether every path from the handler at {@code entry} ends in an athrow of the caught object.
     * The state is the set of local variables that hold the caught object and the number of copies
     * of it on top of the operand stack.
     */
    boolean onlyRethrows(int entry) {
      Deque<State> work = new ArrayDeque<>();
      Set<State> seen = new HashSet<>();
      work.push(new State(entry, new BitSet(), 1));
      while (!work.isEmpty()) {
        State state = work.pop();
        if (!seen.add(state)) {
          continue;
        }
        if (seen.size() > MAX_STATES || state.pc < 0 || state.pc >= code.length) {
          return false;
        }
        int pc = state.pc;
        int op = u1(pc);
        BitSet locals = (BitSet) state.locals.clone();
        int top = state.top;
        switch (op) {
          case 0xac, 0xad, 0xae, 0xaf, 0xb0, 0xb1, 0xa8, 0xa9, 0xc9 -> {
            return false;
          }
          case 0xbf -> {
            if (top == 0) {
              return false;
            }
            continue;
          }
          case 0xa7 -> {
            work.push(new State(pc + (short) u2(pc + 1), locals, top));
            continue;
          }
          case 0xc8 -> {
            work.push(new State(pc + s4(pc + 1), locals, top));
            continue;
          }
          case 0xaa, 0xab -> {
            for (int target : switchTargets(pc)) {
              work.push(new State(target, locals, 0));
            }
            continue;
          }
          default -> {}
        }
        if ((op >= 0x99 && op <= 0xa6) || op == 0xc6 || op == 0xc7) {
          int popped = (op >= 0x9f && op <= 0xa6) ? 2 : 1;
          int after = Math.max(0, top - popped);
          work.push(new State(pc + (short) u2(pc + 1), locals, after));
          work.push(new State(pc + 3, locals, after));
          continue;
        }
        int load = loadIndex(pc);
        int store = storeIndex(pc);
        if (load >= 0) {
          top = locals.get(load) ? top + 1 : 0;
        } else if (store >= 0) {
          boolean reference = isReferenceStore(pc);
          if (reference && top > 0) {
            locals.set(store);
            top--;
          } else {
            locals.clear(store);
            if (isWideStore(pc)) {
              locals.clear(store + 1);
            }
            top = 0;
          }
        } else if (op == 0x59) {
          top = top > 0 ? top + 1 : 0;
        } else if (op == 0x57) {
          top = Math.max(0, top - 1);
        } else if (op != 0xc0 && op != 0x00) {
          top = 0;
        }
        work.push(new State(pc + length(pc), locals, Math.min(top, 4)));
      }
      return true;
    }

    private record State(int pc, BitSet locals, int top) {}

    private int loadIndex(int pc) {
      int op = u1(pc);
      if (op == 0x19) {
        return u1(pc + 1);
      }
      if (op >= 0x2a && op <= 0x2d) {
        return op - 0x2a;
      }
      if (op == 0xc4 && u1(pc + 1) == 0x19) {
        return u2(pc + 2);
      }
      return -1;
    }

    private int storeIndex(int pc) {
      int op = u1(pc);
      if (op >= 0x36 && op <= 0x3a) {
        return u1(pc + 1);
      }
      if (op >= 0x3b && op <= 0x4e) {
        return (op - 0x3b) % 4;
      }
      if (op == 0xc4 && u1(pc + 1) >= 0x36 && u1(pc + 1) <= 0x3a) {
        return u2(pc + 2);
      }
      return -1;
    }

    private boolean isReferenceStore(int pc) {
      int op = u1(pc) == 0xc4 ? u1(pc + 1) : u1(pc);
      return op == 0x3a || (op >= 0x4b && op <= 0x4e);
    }

    private boolean isWideStore(int pc) {
      int op = u1(pc) == 0xc4 ? u1(pc + 1) : u1(pc);
      return op == 0x37 || op == 0x39 || (op >= 0x3f && op <= 0x42) || (op >= 0x47 && op <= 0x4a);
    }

    private List<Integer> switchTargets(int pc) {
      int base = (pc + 4) & ~3;
      List<Integer> targets = new ArrayList<>();
      targets.add(pc + s4(base));
      if (u1(pc) == 0xaa) {
        int low = s4(base + 4);
        int high = s4(base + 8);
        for (int i = 0; i <= high - low; i++) {
          targets.add(pc + s4(base + 12 + 4 * i));
        }
      } else {
        int pairs = s4(base + 4);
        for (int i = 0; i < pairs; i++) {
          targets.add(pc + s4(base + 12 + 8 * i));
        }
      }
      return targets;
    }

    /** The length of the instruction at {@code pc}, operands included. */
    private int length(int pc) {
      int op = u1(pc);
      if (op == 0xaa || op == 0xab) {
        int base = (pc + 4) & ~3;
        if (op == 0xaa) {
          return base - pc + 12 + 4 * (s4(base + 8) - s4(base + 4) + 1);
        }
        return base - pc + 8 + 8 * s4(base + 4);
      }
      if (op == 0xc4) {
        return u1(pc + 1) == 0x84 ? 6 : 4;
      }
      if (op == 0x10
          || op == 0x12
          || (op >= 0x15 && op <= 0x19)
          || (op >= 0x36 && op <= 0x3a)
          || op == 0xa9
          || op == 0xbc) {
        return 2;
      }
      if (op == 0x11
          || op == 0x13
          || op == 0x14
          || op == 0x84
          || (op >= 0x99 && op <= 0xa8)
          || (op >= 0xb2 && op <= 0xb8)
          || op == 0xbb
          || op == 0xbd
          || op == 0xc0
          || op == 0xc1
          || op == 0xc6
          || op == 0xc7) {
        return 3;
      }
      if (op == 0xc5) {
        return 4;
      }
      if (op == 0xb9 || op == 0xba || op == 0xc8 || op == 0xc9) {
        return 5;
      }
      return 1;
    }

    private int u1(int pc) {
      return code[pc] & 0xFF;
    }

    private int u2(int pc) {
      return (u1(pc) << 8) | u1(pc + 1);
    }

    private int s4(int pc) {
      return (u2(pc) << 16) | u2(pc + 2);
    }
  }
}
