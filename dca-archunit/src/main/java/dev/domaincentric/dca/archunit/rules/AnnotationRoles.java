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
   * null}. An entry {@code <annotation>#<attribute>=<value>} matches an annotation of that type
   * whose attribute holds the value - an enum constant of that name, or that string, compared
   * ignoring case; a plain entry matches the annotation type alone.
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
      if (carries(unit.getAnnotations(), type, attribute[0], attribute[1], new HashSet<>())) {
        return entry;
      }
    }
    return null;
  }

  private static boolean carries(
      Iterable<? extends JavaAnnotation<?>> annotations,
      String type,
      String attribute,
      String value,
      Set<String> visited) {
    for (JavaAnnotation<?> annotation : annotations) {
      String name = annotation.getRawType().getName();
      if (name.equals(type) && annotation.get(attribute).map(v -> holds(v, value)).orElse(false)) {
        return true;
      }
      if (visited.add(name)
          && carries(annotation.getRawType().getAnnotations(), type, attribute, value, visited)) {
        return true;
      }
    }
    return false;
  }

  private static boolean holds(Object attributeValue, String value) {
    if (attributeValue instanceof Object[] values) {
      for (Object each : values) {
        if (holds(each, value)) {
          return true;
        }
      }
      return false;
    }
    String actual =
        attributeValue instanceof JavaEnumConstant constant
            ? constant.name()
            : String.valueOf(attributeValue);
    return actual.equalsIgnoreCase(value);
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
