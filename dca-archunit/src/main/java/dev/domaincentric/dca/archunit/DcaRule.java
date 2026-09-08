package dev.domaincentric.dca.archunit;

import com.tngtech.archunit.lang.ArchRule;
import java.util.Objects;
import java.util.Optional;
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
 *
 * <p>Every rule also carries two descriptions of its mechanics, {@link #selects()} and {@link
 * #checks()}: which classes the rule looks at and what it asserts about them, including what does
 * <em>not</em> count. They are mandatory — the factories return a builder that only becomes a
 * {@code DcaRule} once both are given — so a reader of the rule catalog can predict whether a rule
 * applies to a class and what it will say, without reading the rule's source.
 */
public interface DcaRule {

  /** Stable identifier, e.g. {@code DCA-TAC-001}. */
  String id();

  /** Short statement of the rule, e.g. "Aggregate roots must implement AggregateRoot". */
  String title();

  /** Why the rule exists — used as the ArchUnit {@code because(...)} text. */
  String rationale();

  /**
   * Which classes the rule selects, in one to three sentences — the set the assertion runs over,
   * named in terms of the layout ("non-interface classes under {@code <module>.application..} whose
   * simple name ends with the use-case suffix"). A class outside this set is never reported.
   */
  String selects();

  /**
   * What the rule asserts about each selected class, in one to three sentences — including what
   * does not satisfy it ("{@code publish(event)} does not count") and what it does not establish.
   */
  String checks();

  /** Runs the rule; throws {@link AssertionError} on violation. */
  void check(DcaArchitecture architecture);

  /**
   * The single ArchUnit rule behind this check, when there is one.
   *
   * <p>Rules built with {@link #of} expose theirs, which is what makes freezing a baseline and
   * filtering individual violations possible. Rules built with {@link #check} run several checks
   * internally (once per bounded context, say) and return {@link Optional#empty()}.
   */
  default Optional<ArchRule> archRule(DcaArchitecture architecture) {
    return Optional.empty();
  }

  /**
   * A rule built from a single ArchUnit rule derived from the architecture. Complete it with {@link
   * Undescribed#selecting(String)} and {@link Selected#checking(String)}.
   */
  static Undescribed of(
      String id, String title, String rationale, Function<DcaArchitecture, ArchRule> rule) {
    Objects.requireNonNull(rule);
    return new Undescribed(id, title, rationale, null, rule);
  }

  /**
   * A rule with custom check logic (loops over contexts, reflective checks, …). Complete it with
   * {@link Undescribed#selecting(String)} and {@link Selected#checking(String)}.
   */
  static Undescribed check(
      String id, String title, String rationale, Consumer<DcaArchitecture> check) {
    return new Undescribed(id, title, rationale, Objects.requireNonNull(check), null);
  }

  /** A rule whose mechanics are not yet described; not a {@code DcaRule} until they are. */
  final class Undescribed {
    private final String id;
    private final String title;
    private final String rationale;
    private final Consumer<DcaArchitecture> check;
    private final Function<DcaArchitecture, ArchRule> archRule;

    Undescribed(
        String id,
        String title,
        String rationale,
        Consumer<DcaArchitecture> check,
        Function<DcaArchitecture, ArchRule> archRule) {
      this.id = Objects.requireNonNull(id);
      this.title = Objects.requireNonNull(title);
      this.rationale = Objects.requireNonNull(rationale);
      this.check = check;
      this.archRule = archRule;
    }

    /** Names the classes the rule looks at; see {@link DcaRule#selects()}. */
    public Selected selecting(String selects) {
      return new Selected(this, requireText(selects, "selects", id));
    }
  }

  /** A rule with its selection described; {@link #checking(String)} completes it. */
  final class Selected {
    private final Undescribed rule;
    private final String selects;

    Selected(Undescribed rule, String selects) {
      this.rule = rule;
      this.selects = selects;
    }

    /** Names what the rule asserts about each selected class; see {@link DcaRule#checks()}. */
    public DcaRule checking(String checks) {
      return new SimpleRule(
          rule.id,
          rule.title,
          rule.rationale,
          selects,
          requireText(checks, "checks", rule.id),
          rule.check,
          rule.archRule);
    }
  }

  private static String requireText(String text, String field, String id) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException(id + ": " + field + " must not be blank");
    }
    return text.strip();
  }

  /** Default implementation used by the factories. */
  final class SimpleRule implements DcaRule {
    private final String id;
    private final String title;
    private final String rationale;
    private final String selects;
    private final String checks;
    private final Consumer<DcaArchitecture> check;
    private final Function<DcaArchitecture, ArchRule> archRule;

    SimpleRule(
        String id,
        String title,
        String rationale,
        String selects,
        String checks,
        Consumer<DcaArchitecture> check,
        Function<DcaArchitecture, ArchRule> archRule) {
      this.id = Objects.requireNonNull(id);
      this.title = Objects.requireNonNull(title);
      this.rationale = Objects.requireNonNull(rationale);
      this.selects = Objects.requireNonNull(selects);
      this.checks = Objects.requireNonNull(checks);
      this.check = check;
      this.archRule = archRule;
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
    public String selects() {
      return selects;
    }

    @Override
    public String checks() {
      return checks;
    }

    @Override
    public void check(DcaArchitecture architecture) {
      if (check != null) {
        check.accept(architecture);
      } else {
        described(architecture).check(architecture.classes());
      }
    }

    @Override
    public Optional<ArchRule> archRule(DcaArchitecture architecture) {
      return archRule == null ? Optional.empty() : Optional.of(described(architecture));
    }

    private ArchRule described(DcaArchitecture architecture) {
      return archRule.apply(architecture).as(title).because(rationale);
    }

    @Override
    public String toString() {
      return "[" + id + "] " + title;
    }
  }
}
