package dev.domaincentric.dca.archunit.junit;

import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleExecution;
import dev.domaincentric.dca.archunit.DcaRuleOutcome;
import dev.domaincentric.dca.archunit.DcaRuleSelection;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * JUnit 5 base class that runs the DCA rule catalog as dynamic tests — one container per rule set,
 * one test per rule, named {@code [DCA-TAC-001] Aggregate roots must …}.
 *
 * <pre>{@code
 * class ArchitectureTest extends DcaArchitectureTest {
 *   @Override
 *   protected DcaLayout layout() {
 *     return DcaLayout.forBasePackage("com.acme.shop");
 *   }
 * }
 * }</pre>
 *
 * <p>Unless {@link #selection()} is overridden, the configuration comes from {@code
 * dca-archunit.properties} on the test class path — see {@link DcaRuleSelection#fromClasspath()}.
 * Rules that were lowered to {@link dev.domaincentric.dca.archunit.DcaSeverity#WARN} or switched
 * off are reported as aborted tests carrying their recorded reason, so a decision to skip a rule
 * stays visible instead of disappearing from the report.
 *
 * <p>Requires JUnit Jupiter on the test class path (it is a {@code compileOnly} dependency of this
 * library).
 */
public abstract class DcaArchitectureTest {

  private DcaArchitecture architecture;

  /** The layout of the project under test. */
  protected abstract DcaLayout layout();

  /**
   * Which rules run and how strictly: {@code dca-archunit.properties} from the class path, with
   * {@link #additionalSelection()} applied on top.
   *
   * <p>Override {@link #additionalSelection()} to configure rules in code — overriding this method
   * replaces the properties file instead of adding to it.
   */
  protected DcaRuleSelection selection() {
    return DcaRuleSelection.fromClasspath()
        .mergedWith(fromLegacyOverrides())
        .mergedWith(additionalSelection());
  }

  /**
   * Project-specific configuration applied on top of {@code dca-archunit.properties}. Default:
   * none, so the file alone decides.
   *
   * <pre>{@code
   * @Override
   * protected DcaRuleSelection additionalSelection() {
   *   return DcaRuleSelection.all().warning("DCA-TAC-009", "being made final");
   * }
   * }</pre>
   */
  protected DcaRuleSelection additionalSelection() {
    return DcaRuleSelection.all();
  }

  /** Identifiers of rules to skip. Default: none. Prefer {@link #selection()}. */
  protected Set<String> excludedRuleIds() {
    return Set.of();
  }

  /** The rules to run. Default: whatever {@link #selection()} asks for. */
  protected List<DcaRule> rules() {
    return DcaRules.selectFlat(layout(), selection());
  }

  /** The imported architecture; loaded once per test instance. */
  protected DcaArchitecture architecture() {
    if (architecture == null) {
      architecture = DcaArchitecture.load(layout());
    }
    return architecture;
  }

  @TestFactory
  Stream<DynamicNode> dcaRules() {
    DcaArchitecture arch = architecture();
    DcaRuleSelection selection = selection();
    Map<String, List<DcaRule>> bySet = groupBySet(rules());
    return bySet.entrySet().stream()
        .map(
            entry ->
                DynamicContainer.dynamicContainer(
                    entry.getKey() + " (" + entry.getValue().size() + ")",
                    entry.getValue().stream().map(rule -> test(rule, arch, selection)).toList()));
  }

  /** Groups the rules to run by the catalog set they belong to, keeping the catalog's order. */
  private Map<String, List<DcaRule>> groupBySet(List<DcaRule> rules) {
    Map<String, String> setOfRule = new LinkedHashMap<>();
    for (DcaRuleSet set : DcaRules.ruleSets(layout())) {
      set.rules().forEach(rule -> setOfRule.put(rule.id(), set.name()));
    }
    Map<String, List<DcaRule>> bySet = new LinkedHashMap<>();
    DcaRules.setNames().forEach(name -> bySet.put(name, new ArrayList<>()));
    for (DcaRule rule : rules) {
      bySet
          .computeIfAbsent(setOfRule.getOrDefault(rule.id(), "custom"), key -> new ArrayList<>())
          .add(rule);
    }
    bySet.values().removeIf(List::isEmpty);
    return bySet;
  }

  private DynamicTest test(DcaRule rule, DcaArchitecture arch, DcaRuleSelection selection) {
    return DynamicTest.dynamicTest(
        "[" + rule.id() + "] " + rule.title(),
        () -> {
          DcaRuleOutcome outcome = DcaRuleExecution.execute(rule, arch, selection);
          switch (outcome.status()) {
            case FAILED -> throw new AssertionError(outcome.message());
            case WARNED -> Assumptions.abort("WARNING — " + outcome.message());
            case SKIPPED -> Assumptions.abort("switched off — " + outcome.message());
            case PASSED -> {
              // nothing to report
            }
          }
        });
  }

  /** Bridges the older {@link #excludedRuleIds()} hook onto the selection. */
  private DcaRuleSelection fromLegacyOverrides() {
    DcaRuleSelection legacy = DcaRuleSelection.all();
    for (String id : excludedRuleIds()) {
      legacy = legacy.excluding(id);
    }
    return legacy;
  }
}
