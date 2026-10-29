package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;

/**
 * An experimental rule runs only where the selection opts in, and the opt-in leaves every released
 * rule as it was. The rule under test is a fixture, so the mechanism is tested apart from the
 * experimental rules the catalog carries at any given time.
 */
class ExperimentalRulesTest {

  private static final DcaLayout LAYOUT = DcaLayout.forBasePackage("com.acme.shop");

  /** A test-only experimental rule, filed in a set of the catalog as a real one would be. */
  static final DcaRule EXPERIMENTAL =
      DcaRule.check("DCA-USE-900", "Fixture experimental rule", "Exists for the test", arch -> {})
          .experimental()
          .selecting("Nothing; a fixture.")
          .checking("Nothing; a fixture.");

  private static final String SET = "usecase";

  @Test
  void aRuleIsReleasedUnlessDeclaredExperimental() {
    assertEquals(DcaRule.Maturity.EXPERIMENTAL, EXPERIMENTAL.maturity());
    assertEquals(DcaRule.Kind.ENFORCED, EXPERIMENTAL.kind(), "maturity leaves the kind alone");
    assertEquals(DcaRule.Maturity.RELEASED, DcaRules.all(LAYOUT).get(0).maturity());
  }

  /**
   * The experimental rules the catalog ships, in catalog order. Releasing one removes it here and
   * its {@code experimental()} call in the same change.
   */
  @Test
  void theCatalogShipsTheseExperimentalRules() {
    List<String> experimental =
        DcaRules.all(LAYOUT).stream()
            .filter(rule -> rule.maturity() == DcaRule.Maturity.EXPERIMENTAL)
            .map(DcaRule::id)
            .toList();

    assertEquals(
        List.of(
            "DCA-LAY-007",
            "DCA-HEX-014",
            "DCA-HEX-016",
            "DCA-HEX-017",
            "DCA-HEX-018",
            "DCA-HEX-019",
            "DCA-TAC-024",
            "DCA-TAC-025",
            "DCA-MAP-014",
            "DCA-USE-018",
            "DCA-USE-019",
            "DCA-USE-020",
            "DCA-NAM-012",
            "DCA-NAM-013",
            "DCA-ERR-007"),
        experimental);
  }

  @Test
  void executionRunsAnExperimentalRuleLikeAnyOther() {
    String pkg = "dev.domaincentric.dca.archunit.fixtures.naming.bad";
    DcaArchitecture architecture =
        DcaArchitecture.of(
            DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
    DcaRule released =
        DcaRules.all(architecture.layout()).stream()
            .filter(rule -> rule.id().equals("DCA-NAM-001"))
            .findFirst()
            .orElseThrow();
    DcaRule experimental =
        DcaRule.of(
                "DCA-USE-900",
                "Fixture experimental rule",
                "Exists for the test",
                arch -> released.archRule(arch).orElseThrow())
            .experimental()
            .selecting("What DCA-NAM-001 selects; a fixture.")
            .checking("What DCA-NAM-001 checks; a fixture.");
    DcaRuleSelection selection = DcaRuleSelection.all().withExperimental();

    assertTrue(selection.includes(SET, experimental));
    DcaRuleOutcome expected = DcaRuleExecution.execute(released, architecture, selection);
    DcaRuleOutcome outcome = DcaRuleExecution.execute(experimental, architecture, selection);

    assertEquals(DcaRuleOutcome.Status.FAILED, expected.status(), "the fixture classes violate it");
    assertEquals(DcaRuleOutcome.Status.FAILED, outcome.status());
    assertTrue(outcome.message().contains("Architecture Violation"), outcome.message());
    assertFalse(violationLines(outcome).isEmpty(), outcome.message());
    assertEquals(violationLines(expected), violationLines(outcome));
  }

  /**
   * The reported violations, each without the rule id it is prefixed with; the header and the fix
   * line, which name the rule, are left out.
   */
  private static List<String> violationLines(DcaRuleOutcome outcome) {
    return outcome
        .message()
        .lines()
        .map(line -> line.replaceFirst("^\\[DCA-[A-Z]+-\\d+\\] ", ""))
        .filter(line -> line.startsWith("Class <"))
        .toList();
  }

  @Test
  void anInformationalRuleCanBeExperimental() {
    DcaRule rule =
        DcaRule.informational(
                "DCA-USE-901",
                "Fixture diagnostic",
                "Exists for the test",
                arch -> java.util.List.of())
            .experimental()
            .selecting("Nothing; a fixture.")
            .checking("Nothing; a fixture.");

    assertEquals(DcaRule.Kind.INFORMATIONAL, rule.kind());
    assertEquals(DcaRule.Maturity.EXPERIMENTAL, rule.maturity());
  }

  @Test
  void theDefaultSelectionLeavesAnExperimentalRuleOut() {
    assertFalse(DcaRuleSelection.all().includes(SET, EXPERIMENTAL));
    assertFalse(DcaRuleSelection.all().includesExperimental());
    assertFalse(DcaRuleSelection.fromProperties(new Properties()).includes(SET, EXPERIMENTAL));
  }

  @Test
  void withExperimentalIncludesIt() {
    DcaRuleSelection selection = DcaRuleSelection.all().withExperimental();

    assertTrue(selection.includesExperimental());
    assertTrue(selection.includes(SET, EXPERIMENTAL));
  }

  @Test
  void thePropertyIncludesIt() {
    Properties properties = new Properties();
    properties.setProperty("dca.rules.experimental", "true");

    assertTrue(DcaRuleSelection.fromProperties(properties).includes(SET, EXPERIMENTAL));
  }

  @Test
  void thePropertySetToFalseLeavesItOut() {
    Properties properties = new Properties();
    properties.setProperty("dca.rules.experimental", "false");

    assertFalse(DcaRuleSelection.fromProperties(properties).includes(SET, EXPERIMENTAL));
  }

  @Test
  void thePropertyIgnoresCaseAndTreatsBlankAsNoOptIn() {
    Properties upper = new Properties();
    upper.setProperty("dca.rules.experimental", " TRUE ");
    Properties blank = new Properties();
    blank.setProperty("dca.rules.experimental", " ");

    assertTrue(DcaRuleSelection.fromProperties(upper).includesExperimental());
    assertFalse(DcaRuleSelection.fromProperties(blank).includesExperimental());
  }

  @Test
  void thePropertyRejectsAValueOtherThanTrueOrFalse() {
    Properties properties = new Properties();
    properties.setProperty("dca.rules.experimental", "yes");

    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class, () -> DcaRuleSelection.fromProperties(properties));
    assertTrue(failure.getMessage().contains("dca.rules.experimental"), failure.getMessage());
  }

  @Test
  void theOptInComposesWithScopeAndSeverity() {
    DcaRuleSelection selection =
        DcaRuleSelection.all()
            .withExperimental()
            .onlySets("tactical")
            .warning("DCA-TAC-009", "being made final");

    assertTrue(selection.includesExperimental(), "kept through later calls");
    assertFalse(selection.includes(SET, EXPERIMENTAL), "the scope applies to it as well");
    assertTrue(DcaRuleSelection.all().onlySets(SET).withExperimental().includes(SET, EXPERIMENTAL));
    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-TAC-009"));
  }

  @Test
  void eitherSideOfAMergeCanOptIn() {
    DcaRuleSelection optedIn = DcaRuleSelection.all().withExperimental();

    assertTrue(DcaRuleSelection.all().mergedWith(optedIn).includesExperimental());
    assertTrue(optedIn.mergedWith(DcaRuleSelection.all()).includesExperimental());
  }

  @Test
  void aReleasedRuleIsUnaffectedByTheOptIn() {
    DcaRule released = DcaRules.all(LAYOUT).get(0);

    assertEquals(DcaRule.Maturity.RELEASED, released.maturity());
    assertTrue(DcaRuleSelection.all().includes("layered", released));
    assertTrue(DcaRuleSelection.all().withExperimental().includes("layered", released));
    assertEquals(
        DcaRules.selectFlat(LAYOUT, DcaRuleSelection.all()).stream().map(DcaRule::id).toList(),
        DcaRules.selectFlat(LAYOUT, DcaRuleSelection.all().withExperimental()).stream()
            .filter(rule -> rule.maturity() == DcaRule.Maturity.RELEASED)
            .map(DcaRule::id)
            .toList(),
        "the opt-in adds the experimental rules and leaves the released ones as they were");
  }
}
