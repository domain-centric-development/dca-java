package dev.domaincentric.dca.archunit;

import java.util.Objects;
import java.util.Optional;

/**
 * What happened when a rule was evaluated under a {@link DcaRuleSelection}.
 *
 * <p>A skipped or warned rule is not a rule that vanished: it carries the reason it was lowered, so
 * the test report still shows that the rule exists and what the team decided about it.
 *
 * @param ruleId the rule's identifier
 * @param status how the evaluation ended
 * @param message the violation report for {@link Status#FAILED} and {@link Status#WARNED}, the
 *     recorded reason for {@link Status#SKIPPED}, {@code null} when the rule simply passed
 */
public record DcaRuleOutcome(String ruleId, Status status, String message) {

  public DcaRuleOutcome {
    Objects.requireNonNull(ruleId, "ruleId");
    Objects.requireNonNull(status, "status");
  }

  /** How the evaluation of a rule ended. */
  public enum Status {
    /** No violations — or none left after the tolerated ones were filtered out. */
    PASSED,
    /** Violations of a rule at {@link DcaSeverity#ERROR}. */
    FAILED,
    /** Violations of a rule at {@link DcaSeverity#WARN} — reported, but the build stays green. */
    WARNED,
    /** The rule was switched {@link DcaSeverity#OFF} and did not run. */
    SKIPPED
  }

  public Optional<String> messageText() {
    return Optional.ofNullable(message);
  }

  public boolean failed() {
    return status == Status.FAILED;
  }
}
