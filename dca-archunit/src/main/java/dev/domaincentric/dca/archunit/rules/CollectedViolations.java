package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates several ArchUnit rules that together make up one DCA rule and reports <em>all</em> of
 * their violations at once.
 *
 * <p>A DCA rule that iterates over modules — one ArchUnit rule per module — must not stop at the
 * first module that fails: a report naming only the first offender hides the others, and a {@link
 * dev.domaincentric.dca.archunit.DcaRuleSelection} could not tolerate individual violations. So the
 * per-module rules are evaluated, not checked, and their details are thrown together as one {@link
 * DcaRuleViolation}.
 */
final class CollectedViolations {

  private CollectedViolations() {}

  static void check(List<ArchRule> rules, JavaClasses classes) {
    List<String> violations = new ArrayList<>();
    for (ArchRule rule : rules) {
      violations.addAll(rule.evaluate(classes).getFailureReport().getDetails());
    }
    if (!violations.isEmpty()) {
      throw new DcaRuleViolation("", violations);
    }
  }
}
