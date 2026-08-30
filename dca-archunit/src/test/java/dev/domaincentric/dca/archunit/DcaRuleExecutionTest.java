package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DcaRuleExecutionTest {

  private static final String GOOD = "dev.domaincentric.dca.archunit.fixtures.naming.good";
  private static final String BAD = "dev.domaincentric.dca.archunit.fixtures.naming.bad";
  private static final String TACTICAL_BAD = "dev.domaincentric.dca.archunit.fixtures.tactical.bad";

  /** DCA-NAM-001 — an ArchUnit-backed rule, violated by the bad naming fixture. */
  private static final String ARCH_RULE = "DCA-NAM-001";

  /** DCA-TAC-002 — a rule that runs its own checks and exposes no single ArchUnit rule. */
  private static final String CUSTOM_RULE = "DCA-TAC-002";

  private static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  private static DcaRule rule(DcaArchitecture architecture, String id) {
    return DcaRules.all(architecture.layout()).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  private static DcaRuleOutcome execute(String pkg, String ruleId, DcaRuleSelection selection) {
    DcaArchitecture architecture = arch(pkg);
    return DcaRuleExecution.execute(rule(architecture, ruleId), architecture, selection);
  }

  @Test
  void aSatisfiedRulePasses() {
    assertEquals(
        DcaRuleOutcome.Status.PASSED, execute(GOOD, ARCH_RULE, DcaRuleSelection.all()).status());
  }

  @Test
  void aViolatedRuleFails() {
    DcaRuleOutcome outcome = execute(BAD, ARCH_RULE, DcaRuleSelection.all());

    assertEquals(DcaRuleOutcome.Status.FAILED, outcome.status());
    assertTrue(outcome.message().contains("Architecture Violation"), outcome.message());
  }

  @Test
  void aWarningRuleReportsWithoutFailing() {
    DcaRuleOutcome outcome =
        execute(BAD, ARCH_RULE, DcaRuleSelection.all().warning(ARCH_RULE, "being migrated"));

    assertEquals(DcaRuleOutcome.Status.WARNED, outcome.status());
    assertTrue(outcome.message().contains("being migrated"), "the reason travels with the report");
  }

  @Test
  void aSwitchedOffRuleIsSkippedWithItsReason() {
    DcaRuleOutcome outcome =
        execute(BAD, ARCH_RULE, DcaRuleSelection.all().excluding(ARCH_RULE, "no DI framework"));

    assertEquals(DcaRuleOutcome.Status.SKIPPED, outcome.status());
    assertEquals("no DI framework", outcome.message());
  }

  @Test
  void toleratedViolationsAreFilteredOut() {
    DcaRuleOutcome outcome =
        execute(
            BAD,
            ARCH_RULE,
            DcaRuleSelection.all().ignoringViolationsMatching(ARCH_RULE, ".*fixtures\\.naming.*"));

    assertEquals(DcaRuleOutcome.Status.PASSED, outcome.status());
  }

  @Test
  void aToleratedPatternThatMatchesNothingLeavesTheRuleFailing() {
    DcaRuleOutcome outcome =
        execute(
            BAD,
            ARCH_RULE,
            DcaRuleSelection.all().ignoringViolationsMatching(ARCH_RULE, "nothing"));

    assertEquals(DcaRuleOutcome.Status.FAILED, outcome.status());
  }

  @Test
  void toleratedViolationsAlsoWorkForRulesRunningTheirOwnChecks() {
    DcaRuleOutcome enforced = execute(TACTICAL_BAD, CUSTOM_RULE, DcaRuleSelection.all());
    assertEquals(DcaRuleOutcome.Status.FAILED, enforced.status());

    DcaRuleOutcome tolerated =
        execute(
            TACTICAL_BAD,
            CUSTOM_RULE,
            DcaRuleSelection.all()
                .ignoringViolationsMatching(CUSTOM_RULE, ".*fixtures\\.tactical.*"));
    assertEquals(DcaRuleOutcome.Status.PASSED, tolerated.status());
  }

  @Test
  void freezingAcceptsTodaysViolationsAndFailsOnNewOnes(@TempDir Path store) {
    DcaRuleSelection frozen =
        DcaRuleSelection.all().frozen(ARCH_RULE).withFreezeStore(store.resolve("frozen"));

    assertEquals(
        DcaRuleOutcome.Status.PASSED,
        execute(BAD, ARCH_RULE, frozen).status(),
        "first run records the baseline");
    assertEquals(
        DcaRuleOutcome.Status.PASSED,
        execute(BAD, ARCH_RULE, frozen).status(),
        "the same violations stay accepted");
    assertEquals(
        DcaRuleOutcome.Status.FAILED,
        execute(BAD, ARCH_RULE, DcaRuleSelection.all()).status(),
        "without the baseline the rule still fails");
  }

  @Test
  void aRuleWithoutASingleArchUnitRuleCannotBeFrozen() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> execute(TACTICAL_BAD, CUSTOM_RULE, DcaRuleSelection.all().frozen(CUSTOM_RULE)));

    assertTrue(failure.getMessage().contains(CUSTOM_RULE), failure.getMessage());
    assertTrue(failure.getMessage().contains("WARN"), "points at the alternative");
  }
}
