package dev.domaincentric.dca.archunit;

import java.io.Serial;
import java.util.List;
import java.util.Objects;

/**
 * The failure of a rule that collects its violations itself, instead of delegating to a single
 * ArchUnit rule.
 *
 * <p>Keeping the individual violations addressable — rather than folding them into one string — is
 * what lets a {@link DcaRuleSelection} tolerate some of them via {@link
 * DcaRuleSelection#ignoringViolationsMatching(String, String)}.
 */
public final class DcaRuleViolation extends AssertionError {

  @Serial private static final long serialVersionUID = 1L;

  private final transient String header;
  private final transient List<String> violations;

  public DcaRuleViolation(String header, List<String> violations) {
    super(format(header, violations));
    this.header = Objects.requireNonNull(header, "header");
    this.violations = List.copyOf(violations);
  }

  /** What the rule demands, stated once above the list. */
  public String header() {
    return header;
  }

  /** One entry per offending class, field or declaration. */
  public List<String> violations() {
    return violations;
  }

  /**
   * The same failure with only the violations that were not tolerated, or empty when none remain.
   */
  DcaRuleViolation retaining(List<String> remaining) {
    return new DcaRuleViolation(header, remaining);
  }

  private static String format(String header, List<String> violations) {
    if (violations.isEmpty()) {
      return header;
    }
    return header.isBlank()
        ? String.join("\n", violations)
        : header + "\nViolations found:\n" + String.join("\n", violations);
  }
}
