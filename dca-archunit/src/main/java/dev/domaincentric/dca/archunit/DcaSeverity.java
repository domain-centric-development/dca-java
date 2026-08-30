package dev.domaincentric.dca.archunit;

/**
 * How a {@link DcaRule} is treated when it is evaluated.
 *
 * <p>A team adopting the catalog on an existing code base rarely satisfies all rules at once.
 * Rather than deleting a rule from the run — which hides that it was ever considered — it can be
 * lowered to {@link #WARN} or {@link #OFF}. Both stay visible in the test report, together with the
 * reason recorded in the {@link DcaRuleSelection}.
 */
public enum DcaSeverity {

  /** Violations fail the build. The default for every rule. */
  ERROR,

  /** Violations are reported but do not fail the build — the rule is being worked towards. */
  WARN,

  /** The rule is not evaluated at all; it is still listed with its reason. */
  OFF
}
