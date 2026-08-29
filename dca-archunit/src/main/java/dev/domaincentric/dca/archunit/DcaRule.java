package dev.domaincentric.dca.archunit;

import com.tngtech.archunit.lang.ArchRule;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * One governance rule of Domain-Centric Architecture.
 *
 * <p>A rule has a stable identifier ({@code DCA-TAC-003}), a human-readable title, the rationale
 * that appears in violation messages, and a check against a {@link DcaArchitecture}. Most rules
 * wrap a single ArchUnit {@link ArchRule}; some iterate over the discovered bounded contexts and
 * run several checks.
 *
 * <p>Identifiers are the contract shared with the .NET rule library and the DCA knowledge catalog —
 * never renumber them.
 */
public interface DcaRule {

  /** Stable identifier, e.g. {@code DCA-TAC-001}. */
  String id();

  /** Short statement of the rule, e.g. "Aggregate roots must implement AggregateRoot". */
  String title();

  /** Why the rule exists — used as the ArchUnit {@code because(...)} text. */
  String rationale();

  /** Runs the rule; throws {@link AssertionError} on violation. */
  void check(DcaArchitecture architecture);

  /** A rule built from a single ArchUnit rule derived from the architecture. */
  static DcaRule of(
      String id, String title, String rationale, Function<DcaArchitecture, ArchRule> rule) {
    Objects.requireNonNull(rule);
    return new SimpleRule(
        id,
        title,
        rationale,
        arch -> rule.apply(arch).as(title).because(rationale).check(arch.classes()));
  }

  /** A rule with custom check logic (loops over contexts, reflective checks, …). */
  static DcaRule check(String id, String title, String rationale, Consumer<DcaArchitecture> check) {
    return new SimpleRule(id, title, rationale, check);
  }

  /** Default implementation used by the factories. */
  final class SimpleRule implements DcaRule {
    private final String id;
    private final String title;
    private final String rationale;
    private final Consumer<DcaArchitecture> check;

    SimpleRule(String id, String title, String rationale, Consumer<DcaArchitecture> check) {
      this.id = Objects.requireNonNull(id);
      this.title = Objects.requireNonNull(title);
      this.rationale = Objects.requireNonNull(rationale);
      this.check = Objects.requireNonNull(check);
    }

    @Override
    public String id() {
      return id;
    }

    @Override
    public String title() {
      return title;
    }

    @Override
    public String rationale() {
      return rationale;
    }

    @Override
    public void check(DcaArchitecture architecture) {
      check.accept(architecture);
    }

    @Override
    public String toString() {
      return "[" + id + "] " + title;
    }
  }
}
