package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Collects <em>every</em> violation a DCA rule finds before any of them is reported.
 *
 * <p>A rule that checks several things — one condition per declaration, one ArchUnit rule per
 * module or per context-map edge — must not stop at the first finding: a report naming only the
 * first offender hides the others, and a {@link dev.domaincentric.dca.archunit.DcaRuleSelection}
 * that tolerates one violation would then silently pass the rest. So findings are accumulated here
 * and thrown together as one {@link DcaRuleViolation}, whose entries the selection can filter
 * individually.
 */
final class CollectedViolations {

  private final String header;
  private final List<String> violations = new ArrayList<>();

  private CollectedViolations(String header) {
    this.header = Objects.requireNonNull(header, "header");
  }

  /** A collector whose report starts with the given statement of what the rule demands. */
  static CollectedViolations withHeader(String header) {
    return new CollectedViolations(header);
  }

  /** A collector whose report is the bare list of violations. */
  static CollectedViolations withoutHeader() {
    return new CollectedViolations("");
  }

  /** Records one violation. */
  void add(String violation) {
    violations.add(Objects.requireNonNull(violation, "violation"));
  }

  /** Records the violation unless the condition holds. */
  void require(boolean condition, String violation) {
    if (!condition) {
      add(violation);
    }
  }

  /**
   * Evaluates one ArchUnit rule and records each of its violation details, suffixed with the
   * explanation of what the rule was checking — the detail alone ({@code Class A depends on B})
   * does not say why that dependency is wrong.
   */
  void addAll(ArchRule rule, JavaClasses classes, String explanation) {
    for (String detail : rule.evaluate(classes).getFailureReport().getDetails()) {
      add(explanation.isEmpty() ? detail : detail + " - " + explanation);
    }
  }

  /** Evaluates one ArchUnit rule and records its violation details as they are. */
  void addAll(ArchRule rule, JavaClasses classes) {
    addAll(rule, classes, "");
  }

  boolean isEmpty() {
    return violations.isEmpty();
  }

  /**
   * Throws the collected violations as one {@link DcaRuleViolation}; nothing when there are none.
   */
  void throwIfAny() {
    if (!violations.isEmpty()) {
      throw new DcaRuleViolation(header, violations);
    }
  }

  /** Evaluates every rule, then throws all their violations at once. */
  static void check(List<ArchRule> rules, JavaClasses classes) {
    CollectedViolations collected = withoutHeader();
    rules.forEach(rule -> collected.addAll(rule, classes));
    collected.throwIfAny();
  }
}
