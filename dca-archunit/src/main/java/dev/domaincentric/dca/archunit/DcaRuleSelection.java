package dev.domaincentric.dca.archunit;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Which rules of the catalog run, and how strictly.
 *
 * <p>The catalog is opinionated on purpose, but no team adopts all of it on day one — and a rule
 * that a team disagrees with should be a recorded decision, not a reason to drop the library. A
 * selection expresses four things:
 *
 * <ul>
 *   <li><b>scope</b> — {@link #onlySets(String...)} / {@link #onlyIds(String...)} narrow the run
 *   <li><b>severity</b> — {@link #warning(String, String)} reports instead of failing, {@link
 *       #excluding(String, String)} skips; both stay visible in the report with their reason
 *   <li><b>exceptions</b> — {@link #ignoringViolationsMatching(String, String)} tolerates
 *       individual violations of an otherwise enforced rule
 *   <li><b>baselines</b> — {@link #frozen(String...)} accepts today's violations and fails only on
 *       new ones (ArchUnit's {@code FreezingArchRule})
 * </ul>
 *
 * <p>Instances are immutable; every method returns a new selection.
 *
 * <pre>{@code
 * DcaRuleSelection.all()
 *     .excluding("DCA-NAM-002", "no DI framework in this project")
 *     .warning("DCA-TAC-009", "value objects are being made final step by step")
 *     .ignoringViolationsMatching("DCA-STR-003", ".*backoffice.*")
 *     .frozen("DCA-ONI-002");
 * }</pre>
 *
 * <p>The same configuration can live in a {@code dca-archunit.properties} file on the test class
 * path instead — see {@link #fromClasspath()}.
 */
public final class DcaRuleSelection {

  /** Name of the properties resource {@link #fromClasspath()} looks for. */
  public static final String DEFAULT_RESOURCE = "dca-archunit.properties";

  private static final DcaRuleSelection ALL =
      new DcaRuleSelection(null, null, Map.of(), Set.of(), null);

  private final Set<String> includedSets; // null = every set
  private final Set<String> includedIds; // null = every rule
  private final Map<String, RuleSettings> settings;
  private final Set<String> frozenIds;
  private final Path freezeStore;

  private DcaRuleSelection(
      Set<String> includedSets,
      Set<String> includedIds,
      Map<String, RuleSettings> settings,
      Set<String> frozenIds,
      Path freezeStore) {
    this.includedSets = includedSets == null ? null : Set.copyOf(includedSets);
    this.includedIds = includedIds == null ? null : Set.copyOf(includedIds);
    this.settings = Map.copyOf(settings);
    this.frozenIds = Set.copyOf(frozenIds);
    this.freezeStore = freezeStore;
  }

  /** Every rule of every set, all at {@link DcaSeverity#ERROR}. */
  public static DcaRuleSelection all() {
    return ALL;
  }

  // ---------------------------------------------------------------------------------------------
  // Scope
  // ---------------------------------------------------------------------------------------------

  /** Restricts the run to the named rule sets ({@code "tactical"}, {@code "hexagonal"}, …). */
  public DcaRuleSelection onlySets(String... ruleSetNames) {
    Set<String> names = new LinkedHashSet<>(List.of(ruleSetNames));
    names.forEach(DcaRuleSelection::requireKnownSet);
    return new DcaRuleSelection(names, includedIds, settings, frozenIds, freezeStore);
  }

  /** Restricts the run to the given rule identifiers. */
  public DcaRuleSelection onlyIds(String... ruleIds) {
    Set<String> ids = new LinkedHashSet<>(List.of(ruleIds));
    ids.forEach(DcaRuleSelection::requireKnownId);
    return new DcaRuleSelection(includedSets, ids, settings, frozenIds, freezeStore);
  }

  // ---------------------------------------------------------------------------------------------
  // Severity
  // ---------------------------------------------------------------------------------------------

  /**
   * Switches a rule off. Prefer {@link #excluding(String, String)} — the reason is worth writing.
   */
  public DcaRuleSelection excluding(String ruleId) {
    return excluding(ruleId, null);
  }

  /** Switches a rule off, recording why. The reason appears in the test report. */
  public DcaRuleSelection excluding(String ruleId, String reason) {
    return withSeverity(ruleId, DcaSeverity.OFF, reason);
  }

  /** Switches every rule of a set off. */
  public DcaRuleSelection excludingSet(String ruleSetName, String reason) {
    return withSeverityForSet(ruleSetName, DcaSeverity.OFF, reason);
  }

  /** Reports violations of a rule without failing the build. */
  public DcaRuleSelection warning(String ruleId) {
    return warning(ruleId, null);
  }

  /** Reports violations of a rule without failing the build, recording why. */
  public DcaRuleSelection warning(String ruleId, String reason) {
    return withSeverity(ruleId, DcaSeverity.WARN, reason);
  }

  /** Reports violations of every rule of a set without failing the build. */
  public DcaRuleSelection warningForSet(String ruleSetName, String reason) {
    return withSeverityForSet(ruleSetName, DcaSeverity.WARN, reason);
  }

  /** Sets an explicit severity for one rule. */
  public DcaRuleSelection withSeverity(String ruleId, DcaSeverity severity, String reason) {
    requireKnownId(ruleId);
    Objects.requireNonNull(severity, "severity");
    Map<String, RuleSettings> merged = new LinkedHashMap<>(settings);
    RuleSettings current = merged.getOrDefault(ruleId, RuleSettings.enforced());
    merged.put(ruleId, new RuleSettings(severity, reason, current.ignoredViolationPatterns()));
    return new DcaRuleSelection(includedSets, includedIds, merged, frozenIds, freezeStore);
  }

  private DcaRuleSelection withSeverityForSet(
      String ruleSetName, DcaSeverity severity, String reason) {
    requireKnownSet(ruleSetName);
    DcaRuleSelection result = this;
    for (DcaRule rule : DcaRules.ruleSetsByName().get(ruleSetName)) {
      result = result.withSeverity(rule.id(), severity, reason);
    }
    return result;
  }

  // ---------------------------------------------------------------------------------------------
  // Exceptions and baselines
  // ---------------------------------------------------------------------------------------------

  /**
   * Tolerates the violations of one rule whose message matches the regular expression. Use for the
   * documented exception the rule itself cannot express — a legacy package, a generated class.
   */
  public DcaRuleSelection ignoringViolationsMatching(String ruleId, String regex) {
    requireKnownId(ruleId);
    try {
      Pattern.compile(regex);
    } catch (PatternSyntaxException e) {
      throw new IllegalArgumentException(
          "Not a valid regular expression for " + ruleId + ": " + regex, e);
    }
    Map<String, RuleSettings> merged = new LinkedHashMap<>(settings);
    RuleSettings current = merged.getOrDefault(ruleId, RuleSettings.enforced());
    List<String> patterns = new ArrayList<>(current.ignoredViolationPatterns());
    patterns.add(regex);
    merged.put(ruleId, new RuleSettings(current.severity(), current.reason(), patterns));
    return new DcaRuleSelection(includedSets, includedIds, merged, frozenIds, freezeStore);
  }

  /**
   * Freezes the given rules: the violations present at the first run become the accepted baseline,
   * and only new ones fail. Not available for rules that run several checks internally — {@link
   * DcaRule#archRule(DcaArchitecture)} is empty for those, and freezing one fails with a message
   * naming it.
   */
  public DcaRuleSelection frozen(String... ruleIds) {
    Set<String> ids = new LinkedHashSet<>(frozenIds);
    for (String id : ruleIds) {
      requireKnownId(id);
      ids.add(id);
    }
    return new DcaRuleSelection(includedSets, includedIds, settings, ids, freezeStore);
  }

  /** Directory holding the frozen baselines. Default: ArchUnit's own {@code archunit_store}. */
  public DcaRuleSelection withFreezeStore(Path directory) {
    return new DcaRuleSelection(
        includedSets, includedIds, settings, frozenIds, Objects.requireNonNull(directory));
  }

  // ---------------------------------------------------------------------------------------------
  // Combining
  // ---------------------------------------------------------------------------------------------

  /** This selection with {@code other} applied on top — {@code other} wins per rule. */
  public DcaRuleSelection mergedWith(DcaRuleSelection other) {
    Objects.requireNonNull(other, "other");
    Map<String, RuleSettings> merged = new LinkedHashMap<>(settings);
    merged.putAll(other.settings);
    Set<String> frozen = new LinkedHashSet<>(frozenIds);
    frozen.addAll(other.frozenIds);
    return new DcaRuleSelection(
        other.includedSets != null ? other.includedSets : includedSets,
        other.includedIds != null ? other.includedIds : includedIds,
        merged,
        frozen,
        other.freezeStore != null ? other.freezeStore : freezeStore);
  }

  // ---------------------------------------------------------------------------------------------
  // Properties
  // ---------------------------------------------------------------------------------------------

  /**
   * Reads {@value #DEFAULT_RESOURCE} from the class path, or {@link #all()} when there is none.
   *
   * <pre>{@code
   * dca.rules.sets              = tactical,hexagonal
   * dca.rules.off               = DCA-NAM-002,DCA-NAM-006
   * dca.rules.warn              = DCA-TAC-009
   * dca.rules.warn.sets         = naming
   * dca.rules.freeze            = DCA-ONI-002
   * dca.rules.freeze.store      = arch/frozen
   * dca.rule.DCA-NAM-002.reason = no DI framework in this project
   * dca.rule.DCA-STR-003.ignore = .*backoffice.*
   * dca.rule.DCA-STR-003.ignore.1 = .*legacy.*
   * dca.rule.DCA-STR-003.ignore.2 = Generated.{1,3}Client
   * }</pre>
   *
   * <p>The value of an {@code .ignore} key is <em>one</em> regular expression, commas included; a
   * second expression for the same rule uses an indexed key ({@code .ignore.1}, {@code .ignore.2},
   * …, applied after the unindexed one, in numeric order). Lists of rule ids and set names are
   * comma-separated as before.
   *
   * <p>An unknown rule identifier or set name fails immediately — a typo must not silently leave a
   * rule enforced.
   */
  public static DcaRuleSelection fromClasspath() {
    return fromClasspath(DEFAULT_RESOURCE);
  }

  /** Reads the named properties resource from the class path, or {@link #all()} when absent. */
  public static DcaRuleSelection fromClasspath(String resourceName) {
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = DcaRuleSelection.class.getClassLoader();
    }
    try (InputStream in = loader.getResourceAsStream(resourceName)) {
      if (in == null) {
        return all();
      }
      Properties properties = new Properties();
      properties.load(in);
      return fromProperties(properties);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + resourceName + " from the class path", e);
    }
  }

  /** Reads the selection from a properties file. */
  public static DcaRuleSelection fromFile(Path file) {
    try (InputStream in = Files.newInputStream(file)) {
      Properties properties = new Properties();
      properties.load(in);
      return fromProperties(properties);
    } catch (IOException e) {
      throw new UncheckedIOException("Cannot read " + file, e);
    }
  }

  /**
   * Reads the selection from already loaded properties. See {@link #fromClasspath()} for the keys.
   */
  public static DcaRuleSelection fromProperties(Properties properties) {
    Objects.requireNonNull(properties, "properties");
    DcaRuleSelection selection = all();

    List<String> sets = split(properties.getProperty("dca.rules.sets"));
    if (!sets.isEmpty()) {
      selection = selection.onlySets(sets.toArray(String[]::new));
    }
    List<String> ids = split(properties.getProperty("dca.rules.ids"));
    if (!ids.isEmpty()) {
      selection = selection.onlyIds(ids.toArray(String[]::new));
    }
    for (String set : split(properties.getProperty("dca.rules.off.sets"))) {
      selection = selection.excludingSet(set, reasonFor(properties, "set." + set));
    }
    for (String set : split(properties.getProperty("dca.rules.warn.sets"))) {
      selection = selection.warningForSet(set, reasonFor(properties, "set." + set));
    }
    for (String id : split(properties.getProperty("dca.rules.off"))) {
      selection = selection.excluding(id, reasonFor(properties, id));
    }
    for (String id : split(properties.getProperty("dca.rules.warn"))) {
      selection = selection.warning(id, reasonFor(properties, id));
    }
    for (String id : split(properties.getProperty("dca.rules.freeze"))) {
      selection = selection.frozen(id);
    }
    String store = properties.getProperty("dca.rules.freeze.store");
    if (store != null && !store.isBlank()) {
      selection = selection.withFreezeStore(Path.of(store.trim()));
    }
    for (Map.Entry<String, List<String>> entry : ignoreExpressions(properties).entrySet()) {
      for (String regex : entry.getValue()) {
        selection = selection.ignoringViolationsMatching(entry.getKey(), regex);
      }
    }
    return selection;
  }

  /** {@code dca.rule.<id>.ignore} and {@code dca.rule.<id>.ignore.<n>}. */
  private static final Pattern IGNORE_KEY =
      Pattern.compile("dca\\.rule\\.(.+?)\\.ignore(?:\\.(\\d+))?");

  /**
   * The ignore expressions per rule id, in the order: the unindexed key first, then the indexed
   * keys by number. Each value is one regular expression, taken as written — a comma is part of the
   * expression ({@code Foo.{1,3}Bar}), never a separator.
   */
  private static Map<String, List<String>> ignoreExpressions(Properties properties) {
    Map<String, java.util.TreeMap<Integer, String>> byRule = new java.util.TreeMap<>();
    for (String key : properties.stringPropertyNames()) {
      java.util.regex.Matcher matcher = IGNORE_KEY.matcher(key);
      if (!matcher.matches()) {
        continue;
      }
      String value = properties.getProperty(key);
      if (value == null || value.isBlank()) {
        continue;
      }
      int index = matcher.group(2) == null ? -1 : Integer.parseInt(matcher.group(2));
      byRule
          .computeIfAbsent(matcher.group(1), id -> new java.util.TreeMap<>())
          .put(index, value.trim());
    }
    Map<String, List<String>> ordered = new LinkedHashMap<>();
    byRule.forEach((id, expressions) -> ordered.put(id, List.copyOf(expressions.values())));
    return ordered;
  }

  private static String reasonFor(Properties properties, String id) {
    return properties.getProperty("dca.rule." + id + ".reason");
  }

  private static List<String> split(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }
    List<String> parts = new ArrayList<>();
    for (String part : value.split(",")) {
      if (!part.isBlank()) {
        parts.add(part.trim());
      }
    }
    return parts;
  }

  // ---------------------------------------------------------------------------------------------
  // Reading the selection
  // ---------------------------------------------------------------------------------------------

  /** Whether a rule of the given set takes part in the run at all. */
  public boolean includes(String ruleSetName, String ruleId) {
    if (includedSets != null && !includedSets.contains(ruleSetName)) {
      return false;
    }
    return includedIds == null || includedIds.contains(ruleId);
  }

  /** The severity configured for a rule, {@link DcaSeverity#ERROR} unless lowered. */
  public DcaSeverity severityOf(String ruleId) {
    return settings.getOrDefault(ruleId, RuleSettings.enforced()).severity();
  }

  /** Why a rule was lowered or switched off, if a reason was recorded. */
  public Optional<String> reasonFor(String ruleId) {
    return Optional.ofNullable(settings.get(ruleId)).map(RuleSettings::reason);
  }

  /** Regular expressions whose matching violations are tolerated for this rule. */
  public List<String> ignoredViolationPatterns(String ruleId) {
    return settings.getOrDefault(ruleId, RuleSettings.enforced()).ignoredViolationPatterns();
  }

  /** Whether this rule runs against a frozen baseline. */
  public boolean isFrozen(String ruleId) {
    return frozenIds.contains(ruleId);
  }

  /** The configured baseline directory, if any. */
  public Optional<Path> freezeStore() {
    return Optional.ofNullable(freezeStore);
  }

  /** Severity, reason and tolerated violations of a single rule. */
  public record RuleSettings(
      DcaSeverity severity, String reason, List<String> ignoredViolationPatterns) {

    public RuleSettings {
      Objects.requireNonNull(severity, "severity");
      ignoredViolationPatterns = List.copyOf(ignoredViolationPatterns);
    }

    static RuleSettings enforced() {
      return new RuleSettings(DcaSeverity.ERROR, null, List.of());
    }
  }

  private static void requireKnownId(String ruleId) {
    if (!DcaRules.allIds().contains(ruleId) && !DcaRules.retired().containsKey(ruleId)) {
      throw new IllegalArgumentException(
          "Unknown rule id: " + ruleId + ". See RULES.md for the catalog.");
    }
  }

  private static void requireKnownSet(String ruleSetName) {
    if (!DcaRules.setNames().contains(ruleSetName)) {
      throw new IllegalArgumentException(
          "Unknown rule set: "
              + ruleSetName
              + ". Known sets: "
              + String.join(", ", DcaRules.setNames()));
    }
  }

  @Override
  public String toString() {
    List<String> parts = new ArrayList<>();
    parts.add("retired " + DcaRules.retired().keySet());
    parts.add(includedSets == null ? "all sets" : "sets " + includedSets);
    if (includedIds != null) {
      parts.add("ids " + includedIds);
    }
    if (!settings.isEmpty()) {
      parts.add(settings.size() + " rule(s) configured");
    }
    if (!frozenIds.isEmpty()) {
      parts.add(frozenIds.size() + " frozen");
    }
    return "DcaRuleSelection[" + String.join(", ", parts) + "]";
  }
}
