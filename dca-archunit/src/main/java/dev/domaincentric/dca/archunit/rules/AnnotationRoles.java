package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns a role of {@code FrameworkAnnotations} — a list of fully qualified annotation names,
 * possibly empty — into the ArchUnit predicates and conditions the rules compose. An empty role
 * matches nothing: a rule that forbids the role has nothing to forbid, a rule that requires it
 * selects nothing.
 */
final class AnnotationRoles {

  private AnnotationRoles() {}

  /** Directly annotated with any annotation of the role; never true for an empty role. */
  static DescribedPredicate<CanBeAnnotated> annotatedWithAny(List<String> role) {
    if (role.isEmpty()) {
      return DescribedPredicate.<CanBeAnnotated>alwaysFalse()
          .as("annotated with a configured annotation (none configured)");
    }
    DescribedPredicate<CanBeAnnotated> predicate =
        CanBeAnnotated.Predicates.annotatedWith(role.get(0));
    for (String fqn : role.subList(1, role.size())) {
      predicate = predicate.or(CanBeAnnotated.Predicates.annotatedWith(fqn));
    }
    return predicate.as("annotated with any of " + role);
  }

  /** Directly annotated with any annotation of any of the roles. */
  @SafeVarargs
  static DescribedPredicate<CanBeAnnotated> annotatedWithAny(List<String>... roles) {
    DescribedPredicate<CanBeAnnotated> predicate = null;
    for (List<String> role : roles) {
      if (role.isEmpty()) {
        continue;
      }
      predicate = predicate == null ? annotatedWithAny(role) : predicate.or(annotatedWithAny(role));
    }
    return predicate == null
        ? DescribedPredicate.<CanBeAnnotated>alwaysFalse()
            .as("annotated with a configured annotation (none configured)")
        : predicate;
  }

  /**
   * Condition: the class is directly annotated with any annotation of any of the roles. For an
   * empty selection of roles the condition records no event, so {@code noClasses().should(...)}
   * passes.
   */
  @SafeVarargs
  static ArchCondition<JavaClass> beAnnotatedWithAny(List<String>... roles) {
    List<String> all = new ArrayList<>();
    for (List<String> role : roles) {
      for (String fqn : role) {
        if (!all.contains(fqn)) {
          all.add(fqn);
        }
      }
    }
    if (all.isEmpty()) {
      return new ArchCondition<>("be annotated with a configured annotation (none configured)") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
          // nothing configured, nothing to record
        }
      };
    }
    ArchCondition<JavaClass> condition = ArchConditions.beAnnotatedWith(all.get(0));
    for (String fqn : all.subList(1, all.size())) {
      condition = condition.or(ArchConditions.beAnnotatedWith(fqn));
    }
    return condition;
  }

  /** {@code alwaysTrue} when the role is configured, {@code alwaysFalse} otherwise. */
  static DescribedPredicate<JavaClass> whenConfigured(List<String> role) {
    return role.isEmpty()
        ? DescribedPredicate.<JavaClass>alwaysFalse()
            .as("a configured annotation exists (none does)")
        : DescribedPredicate.<JavaClass>alwaysTrue().as("a configured annotation exists");
  }

  /**
   * The first entry of the role the code unit carries, directly or as a meta-annotation, or {@code
   * null}. A plain entry matches the annotation type alone. An entry {@code
   * <annotation>#<attribute>=<value>} matches a code unit carrying an annotation of that type when
   * the attribute holds the value - an enum constant of that name, or that string, compared
   * ignoring case; an entry {@code <annotation>#<attribute>=} matches when the attribute holds no
   * value. The values are those of every annotation of that type on the code unit together with
   * those on its declaring class, as a framework combines a class-level and a method-level mapping;
   * the class alone makes no match.
   */
  static String matchingEntry(JavaCodeUnit unit, List<String> role) {
    for (String entry : role) {
      int hash = entry.indexOf('#');
      if (hash < 0) {
        if (unit.isMetaAnnotatedWith(entry)) {
          return entry;
        }
        continue;
      }
      String type = entry.substring(0, hash);
      String[] attribute = entry.substring(hash + 1).split("=", 2);
      Set<String> values = new HashSet<>();
      if (!collect(unit.getAnnotations(), type, attribute[0], values, new HashSet<>())) {
        continue;
      }
      collect(unit.getOwner().getAnnotations(), type, attribute[0], values, new HashSet<>());
      boolean matches =
          attribute[1].isEmpty()
              ? values.isEmpty()
              : values.stream().anyMatch(v -> v.equalsIgnoreCase(attribute[1]));
      if (matches) {
        return entry;
      }
    }
    return null;
  }

  /**
   * Adds the attribute values of every annotation of the type among the annotations, directly or as
   * a meta-annotation; true when at least one such annotation was found.
   */
  private static boolean collect(
      Iterable<? extends JavaAnnotation<?>> annotations,
      String type,
      String attribute,
      Set<String> values,
      Set<String> visited) {
    boolean found = false;
    for (JavaAnnotation<?> annotation : annotations) {
      String name = annotation.getRawType().getName();
      if (name.equals(type)) {
        found = true;
        annotation.get(attribute).ifPresent(v -> addValues(v, values));
      }
      if (visited.add(name)
          && collect(annotation.getRawType().getAnnotations(), type, attribute, values, visited)) {
        found = true;
      }
    }
    return found;
  }

  private static void addValues(Object attributeValue, Set<String> values) {
    if (attributeValue instanceof Object[] array) {
      for (Object each : array) {
        addValues(each, values);
      }
      return;
    }
    values.add(
        attributeValue instanceof JavaEnumConstant constant
            ? constant.name()
            : String.valueOf(attributeValue));
  }

  /** Meta-annotated with any annotation of the role; false for an empty role. */
  static boolean isMetaAnnotatedWithAny(CanBeAnnotated item, List<String> role) {
    for (String fqn : role) {
      if (item.isMetaAnnotatedWith(fqn)) {
        return true;
      }
    }
    return false;
  }
}
