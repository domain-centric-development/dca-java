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
import java.util.List;
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

  /** Convenience: run every rule against the architecture, failing on the first violation. */
  public static void checkAll(DcaArchitecture architecture) {
    all(architecture.layout()).forEach(r -> r.check(architecture));
  }

  static Stream<DcaRule> stream(DcaLayout layout) {
    return all(layout).stream();
  }
}
