package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * The method calls of one class to its own methods — the directed graph a use case forms when
 * {@code execute} delegates to a private helper.
 *
 * <p>The transaction rules reason along this graph's direction: what a method reaches (its callees,
 * transitively) is what runs when it runs; the units that reach it are the paths it can be entered
 * on. A helper two entry methods both call is reached from both, but connects neither to the other
 * — a shared callee is not an execution path. Calls from lambda bodies are attributed by ArchUnit
 * to the method that declares the lambda, so the graph is one of methods, not of blocks — which is
 * also its limit.
 */
final class IntraClassCalls {

  private final Map<JavaCodeUnit, Set<JavaCodeUnit>> callers = new HashMap<>();
  private final Map<JavaCodeUnit, Set<JavaCodeUnit>> callees = new HashMap<>();

  IntraClassCalls(JavaClass owner) {
    for (JavaCodeUnit unit : owner.getCodeUnits()) {
      for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
        if (!call.getTargetOwner().equals(owner)) {
          continue;
        }
        call.getTarget()
            .resolveMember()
            .ifPresent(
                target -> {
                  callers.computeIfAbsent(target, t -> new LinkedHashSet<>()).add(unit);
                  callees.computeIfAbsent(unit, u -> new LinkedHashSet<>()).add(target);
                });
      }
    }
  }

  /** The unit itself and every unit that reaches it through calls within the class. */
  Set<JavaCodeUnit> callersOf(JavaCodeUnit unit) {
    return closure(unit, callers);
  }

  /** The unit itself and every unit it reaches through calls within the class. */
  Set<JavaCodeUnit> reachableFrom(JavaCodeUnit unit) {
    return closure(unit, callees);
  }

  /**
   * The paths a unit can be entered on: those of its (transitive) callers - the unit itself
   * included - that are entry points. A unit is an entry point when it can be called from outside
   * the class (any code unit that is not private, synthetic or a bridge - a public method stays an
   * entry point even when another method of the class also calls it) or when no unit of the class
   * calls it. When the unit is reached only from within a cycle of private helpers, so that no
   * caller qualifies, the unit itself is taken as the entry point.
   */
  Set<JavaCodeUnit> entryPointsOf(JavaCodeUnit unit) {
    Set<JavaCodeUnit> roots = new LinkedHashSet<>();
    for (JavaCodeUnit caller : callersOf(unit)) {
      if (isEntryPoint(caller)) {
        roots.add(caller);
      }
    }
    if (roots.isEmpty()) {
      roots.add(unit);
    }
    return roots;
  }

  /**
   * The units reachable from {@code start} on routes that pass only through units satisfying {@code
   * through} - {@code start} included, and only if it satisfies it too. A unit that fails the
   * predicate is not entered, so nothing behind it is reached on that route (it may still be
   * reached on another). Cycle-safe.
   */
  Set<JavaCodeUnit> reachableThrough(JavaCodeUnit start, Predicate<JavaCodeUnit> through) {
    Set<JavaCodeUnit> reached = new LinkedHashSet<>();
    Deque<JavaCodeUnit> pending = new ArrayDeque<>();
    pending.add(start);
    while (!pending.isEmpty()) {
      JavaCodeUnit current = pending.remove();
      if (through.test(current) && reached.add(current)) {
        pending.addAll(callees.getOrDefault(current, Set.of()));
      }
    }
    return reached;
  }

  private boolean isEntryPoint(JavaCodeUnit unit) {
    Set<JavaModifier> modifiers = unit.getModifiers();
    boolean externallyCallable =
        !modifiers.contains(JavaModifier.PRIVATE)
            && !modifiers.contains(JavaModifier.SYNTHETIC)
            && !modifiers.contains(JavaModifier.BRIDGE);
    return externallyCallable || callers.getOrDefault(unit, Set.of()).isEmpty();
  }

  private static Set<JavaCodeUnit> closure(
      JavaCodeUnit start, Map<JavaCodeUnit, Set<JavaCodeUnit>> edges) {
    Set<JavaCodeUnit> reached = new LinkedHashSet<>();
    Deque<JavaCodeUnit> pending = new ArrayDeque<>();
    pending.add(start);
    while (!pending.isEmpty()) {
      JavaCodeUnit current = pending.remove();
      if (reached.add(current)) {
        pending.addAll(edges.getOrDefault(current, Set.of()));
      }
    }
    return reached;
  }
}
