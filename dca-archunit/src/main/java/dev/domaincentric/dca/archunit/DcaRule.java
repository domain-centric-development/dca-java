package dev.domaincentric.dca.archunit;

import com.tngtech.archunit.lang.ArchRule;
import java.util.List;
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

  enum Kind {
    ENFORCED,
    INFORMATIONAL
  }

  /**
   * What an informational rule observed, in reading order — empty for every enforced rule and for a
   * diagnostic that found nothing. It asserts nothing, so this never fails a build; {@link
   * DcaRuleExecution} carries the lines in the rule's outcome.
   */
  default List<String> observe(DcaArchitecture architecture) {
    return List.of();
  }

  /** Whether this entry asserts policy or only reports information. */
  default Kind kind() {
    return Kind.ENFORCED;
  }

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

  /**
   * What to change to satisfy the rule, in one imperative sentence, when the rule can name it —
   * "rename the class to *UseCase", "break the cycle by moving the shared concept behind a port".
   * {@link DcaRuleExecution} appends it to the violation report as a {@code Fix:} line, so the
   * remedy reaches the reader for rules built with {@link #of} as well, whose ArchUnit report says
   * only what is wrong. The counterpart of the {@code fix} argument of .NET's {@code DcaRule.Fail}.
   */
  default Optional<String> remedy() {
    return Optional.empty();
  }

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

  /**
   * A diagnostic entry, counted separately from enforced rules. It asserts nothing, so it does not
   * throw: what it observes it returns, and {@link DcaRuleExecution} carries the lines in the
   * rule's outcome. Returning an empty list means there was nothing to observe.
   */
  static Undescribed informational(
      String id, String title, String rationale, Function<DcaArchitecture, List<String>> report) {
    Objects.requireNonNull(report, "report");
    Undescribed result = check(id, title, rationale, architecture -> {});
    result.kind = Kind.INFORMATIONAL;
    result.report = report;
    return result;
  }

  /** A rule whose mechanics are not yet described; not a {@code DcaRule} until they are. */
  final class Undescribed {
    private Kind kind = Kind.ENFORCED;
    private Function<DcaArchitecture, List<String>> report;
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
      return checking(checks, null);
    }

    /**
     * The same, with the one imperative sentence that says what to change; see {@link
     * DcaRule#remedy()}.
     */
    public DcaRule checking(String checks, String remedy) {
      return new SimpleRule(
          rule.id,
          rule.title,
          rule.rationale,
          selects,
          requireText(checks, "checks", rule.id),
          remedy == null ? null : requireText(remedy, "remedy", rule.id),
          rule.check,
          rule.archRule,
          rule.kind,
          rule.report);
    }
  }

  /**
   * The rationale as it reads after "because": the first letter lower-cased, acronyms untouched.
   */
  private static String uncapitalised(String text) {
    if (text.isEmpty() || !Character.isUpperCase(text.charAt(0))) {
      return text;
    }
    // "DTOs are …" or "HTTP adapters …" keep their capital; only an ordinary word is lowered.
    if (text.length() > 1 && Character.isUpperCase(text.charAt(1))) {
      return text;
    }
    return Character.toLowerCase(text.charAt(0)) + text.substring(1);
  }

  private static String requireText(String text, String field, String id) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException(id + ": " + field + " must not be blank");
    }
    return text.strip();
  }

  /** Default implementation used by the factories. */
  final class SimpleRule implements DcaRule {
    private final Kind kind;

    @Override
    public Kind kind() {
      return kind;
    }

    @Override
    public List<String> observe(DcaArchitecture architecture) {
      return report == null ? List.of() : List.copyOf(report.apply(architecture));
    }

    private final String id;
    private final String title;
    private final String rationale;
    private final String selects;
    private final String checks;
    private final String remedy;
    private final Consumer<DcaArchitecture> check;
    private final Function<DcaArchitecture, ArchRule> archRule;
    private final Function<DcaArchitecture, List<String>> report;

    SimpleRule(
        String id,
        String title,
        String rationale,
        String selects,
        String checks,
        String remedy,
        Consumer<DcaArchitecture> check,
        Function<DcaArchitecture, ArchRule> archRule,
        Kind kind,
        Function<DcaArchitecture, List<String>> report) {
      this.kind = kind;
      this.report = report;
      this.id = Objects.requireNonNull(id);
      this.title = Objects.requireNonNull(title);
      this.rationale = Objects.requireNonNull(rationale);
      this.selects = Objects.requireNonNull(selects);
      this.checks = Objects.requireNonNull(checks);
      this.remedy = remedy;
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
    public Optional<String> remedy() {
      return Optional.ofNullable(remedy);
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
      // ArchUnit renders "<title>, because <rationale>". The rationale is a sentence of its own in
      // rules.json and in the catalog, so it starts with a capital there; inside this sentence that
      // reads as "because A domain service exists …". Lower-casing the first letter here keeps both
      // readings right without touching 120 texts.
      return archRule.apply(architecture).as(title).because(uncapitalised(rationale));
    }

    @Override
    public String toString() {
      return "[" + id + "] " + title;
    }
  }
}
