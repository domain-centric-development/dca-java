package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.Source;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * The classes a compiled class names in its constant pool — every class a {@code checkcast}, an
 * {@code instanceof}, a class literal, a member access or a bootstrap argument refers to.
 *
 * <p>ArchUnit's import model leaves out two references a pattern-matching {@code switch} compiles
 * to: the cast to a case type and the case types handed to the type-switch bootstrap. A {@code case
 * Refused r -> …} that never touches {@code r} is therefore invisible to it. The constant pool
 * holds both, so a rule that has to see a switch over cases reads it from the class file. Only the
 * pool is read — no byte code is interpreted — and a class whose file cannot be opened yields
 * {@link Optional#empty()}.
 */
final class ClassFileReferences {

  private static final int CLASS = 7;
  private static final int UTF8 = 1;

  private ClassFileReferences() {}

  /** The binary names of the classes the class file names, or empty when it cannot be read. */
  static Optional<Set<String>> of(JavaClass type) {
    Optional<Source> source = type.getSource();
    if (source.isEmpty()) {
      return Optional.empty();
    }
    try (InputStream in = source.get().getUri().toURL().openStream()) {
      return Optional.of(read(new DataInputStream(new BufferedInputStream(in))));
    } catch (IOException | IllegalArgumentException unreadable) {
      return Optional.empty();
    }
  }

  private static Set<String> read(DataInputStream in) throws IOException {
    if (in.readInt() != 0xCAFEBABE) {
      throw new IOException("not a class file");
    }
    in.readUnsignedShort();
    in.readUnsignedShort();
    int count = in.readUnsignedShort();
    Map<Integer, String> utf8 = new HashMap<>();
    Set<Integer> classNameIndices = new HashSet<>();
    for (int index = 1; index < count; index++) {
      int tag = in.readUnsignedByte();
      switch (tag) {
        case UTF8 -> utf8.put(index, in.readUTF());
        case CLASS -> classNameIndices.add(in.readUnsignedShort());
        case 8, 16, 19, 20 -> in.skipNBytes(2);
        case 15 -> in.skipNBytes(3);
        case 3, 4, 9, 10, 11, 12, 17, 18 -> in.skipNBytes(4);
        case 5, 6 -> {
          in.skipNBytes(8);
          index++;
        }
        default -> throw new IOException("unknown constant pool tag " + tag);
      }
    }
    Set<String> names = new HashSet<>();
    for (int nameIndex : classNameIndices) {
      String name = utf8.get(nameIndex);
      if (name == null) {
        continue;
      }
      String element = name;
      if (name.startsWith("[")) {
        element = name.replaceFirst("^\\[+", "");
        if (!element.startsWith("L")) {
          continue;
        }
        element = element.substring(1, element.length() - 1);
      }
      names.add(element.replace('/', '.'));
    }
    return names;
  }
}
