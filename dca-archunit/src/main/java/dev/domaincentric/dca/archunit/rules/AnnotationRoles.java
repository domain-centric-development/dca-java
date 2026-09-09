package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import java.util.ArrayList;
import java.util.List;

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
