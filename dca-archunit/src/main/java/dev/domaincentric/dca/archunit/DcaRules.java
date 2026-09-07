package dev.domaincentric.dca.archunit;

import dev.domaincentric.dca.archunit.rules.AdvancedPatternRules;
import dev.domaincentric.dca.archunit.rules.ContextMapRules;
import dev.domaincentric.dca.archunit.rules.CycleRules;
import dev.domaincentric.dca.archunit.rules.HexagonalRules;
import dev.domaincentric.dca.archunit.rules.LayeredRules;
import dev.domaincentric.dca.archunit.rules.NamingRules;
import dev.domaincentric.dca.archunit.rules.OnionRules;
import dev.domaincentric.dca.archunit.rules.StrategicPatternRules;
import dev.domaincentric.dca.archunit.rules.TacticalPatternRules;
import dev.domaincentric.dca.archunit.rules.UseCaseRules;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Entry point to the DCA rule catalog.
 *
 * <pre>{@code
 * DcaArchitecture arch = DcaArchitecture.load(DcaLayout.forBasePackage("com.acme.shop"));
 * for (DcaRule rule : DcaRules.all(arch.layout())) {
 *   rule.check(arch);
 * }
 * }</pre>
 *
 * <p>For JUnit 5, extend {@link dev.domaincentric.dca.archunit.junit.DcaArchitectureTest} instead —
 * it turns every rule into a dynamic test.
 */
public final class DcaRules {

  /**
   * Layout used to enumerate the catalog itself. Rule identifiers, titles and set names do not
   * depend on the project's packages, so any base package will do.
   */
  private static final DcaLayout CATALOG_LAYOUT = DcaLayout.forBasePackage("com.example");

  private DcaRules() {}

  /** All rule sets, in catalog order. */
  public static List<DcaRuleSet> ruleSets(DcaLayout layout) {
    return List.of(
        new LayeredRules(layout),
        new OnionRules(layout),
        new HexagonalRules(layout),
        new TacticalPatternRules(layout),
        new StrategicPatternRules(layout),
        new ContextMapRules(layout),
        new AdvancedPatternRules(layout),
        new UseCaseRules(layout),
        new NamingRules(layout),
        new CycleRules(layout));
  }

  /** Every rule of every set. */
  public static List<DcaRule> all(DcaLayout layout) {
    return ruleSets(layout).stream().flatMap(s -> s.rules().stream()).toList();
  }

  /** Every rule except the given identifiers. */
  public static List<DcaRule> allExcept(DcaLayout layout, Set<String> excludedIds) {
    return all(layout).stream().filter(r -> !excludedIds.contains(r.id())).toList();
  }

  /** The rules of the named sets only ({@code "tactical"}, {@code "hexagonal"}, …). */
  public static List<DcaRule> only(DcaLayout layout, String... ruleSetNames) {
    Set<String> names = Set.of(ruleSetNames);
    return ruleSets(layout).stream()
        .filter(s -> names.contains(s.name()))
        .flatMap(s -> s.rules().stream())
        .toList();
  }

  /** The sets and rules the selection asks for, in catalog order, empty sets removed. */
  public static List<DcaRuleSet> select(DcaLayout layout, DcaRuleSelection selection) {
    List<DcaRuleSet> selected = new ArrayList<>();
    for (DcaRuleSet set : ruleSets(layout)) {
      List<DcaRule> rules =
          set.rules().stream().filter(r -> selection.includes(set.name(), r.id())).toList();
      if (!rules.isEmpty()) {
        selected.add(new SelectedRuleSet(set.name(), rules));
      }
    }
    return selected;
  }

  /** The rules the selection asks for, flattened. */
  public static List<DcaRule> selectFlat(DcaLayout layout, DcaRuleSelection selection) {
    return select(layout, selection).stream().flatMap(s -> s.rules().stream()).toList();
  }

  /** Convenience: run every rule against the architecture, failing on the first violation. */
  public static void checkAll(DcaArchitecture architecture) {
    checkAll(architecture, DcaRuleSelection.all());
  }

  /**
   * Runs the selected rules, failing on the first rule at {@link DcaSeverity#ERROR} that is
   * violated. Warnings go to {@code System.err}, skipped rules are silent.
   */
  public static void checkAll(DcaArchitecture architecture, DcaRuleSelection selection) {
    for (DcaRule rule : selectFlat(architecture.layout(), selection)) {
      DcaRuleOutcome outcome = DcaRuleExecution.execute(rule, architecture, selection);
      switch (outcome.status()) {
        case FAILED -> throw new AssertionError("[" + rule.id() + "] " + outcome.message());
        case WARNED -> System.err.println("[" + rule.id() + "] WARNING: " + outcome.message());
        case PASSED, SKIPPED -> {
          // nothing to report
        }
      }
    }
  }

  /** Every rule identifier of the catalog, in catalog order. Unmodifiable. */
  public static Set<String> allIds() {
    return Catalog.IDS;
  }

  /** The names of the ten rule sets, in catalog order. Unmodifiable. */
  public static Set<String> setNames() {
    return Catalog.BY_SET.keySet();
  }

  /** The catalog keyed by set name — used to resolve set-wide configuration to rule identifiers. */
  static Map<String, List<DcaRule>> ruleSetsByName() {
    return Catalog.BY_SET;
  }

  /**
   * The catalog's metadata — ids and set names — built once. Every {@code DcaRuleSelection} setting
   * validates its id against it, and a set-wide setting resolves every id of the set; rebuilding
   * ten rule sets for each of those lookups is wasted work.
   */
  private static final class Catalog {
    static final Map<String, List<DcaRule>> BY_SET;
    static final Set<String> IDS;

    static {
      Map<String, List<DcaRule>> bySet = new LinkedHashMap<>();
      Set<String> ids = new LinkedHashSet<>();
      for (DcaRuleSet set : ruleSets(CATALOG_LAYOUT)) {
        bySet.put(set.name(), set.rules());
        set.rules().forEach(rule -> ids.add(rule.id()));
      }
      BY_SET = Collections.unmodifiableMap(bySet);
      IDS = Collections.unmodifiableSet(ids);
    }
  }

  private record SelectedRuleSet(String name, List<DcaRule> rules) implements DcaRuleSet {}

  static Stream<DcaRule> stream(DcaLayout layout) {
    return all(layout).stream();
  }
}
