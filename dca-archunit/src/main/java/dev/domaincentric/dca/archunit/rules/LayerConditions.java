package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.conditions.ArchConditions;

/**
 * {@link ArchConditions#be} for the layout's layer and role predicates. Their descriptions read as
 * a selection ("classes that are in the domain layer"); as a condition the verb goes, so a
 * violation reads "X is not in the domain layer" instead of "X is not are in the domain layer".
 */
final class LayerConditions {

  private LayerConditions() {}

  static ArchCondition<JavaClass> be(DescribedPredicate<? super JavaClass> predicate) {
    return ArchConditions.be(predicate.as(asCondition(predicate.getDescription())));
  }

  static ArchCondition<JavaClass> notBe(DescribedPredicate<? super JavaClass> predicate) {
    return ArchConditions.not(be(predicate));
  }

  static String asCondition(String description) {
    String phrase = description.replace(" or are ", " or ").replace(" and are ", " and ");
    return phrase.startsWith("are ") ? phrase.substring("are ".length()) : phrase;
  }
}
