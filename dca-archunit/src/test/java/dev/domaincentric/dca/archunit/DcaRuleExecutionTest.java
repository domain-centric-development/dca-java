package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Nested;
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

  /**
   * The baseline is taken from one state of the code and a later state is judged against it. Here
   * the baseline is a fixture without violations and the later state a fixture with them, so every
   * violation is new — the same store, a different architecture.
   */
  @Test
  void freezingAcceptsTodaysViolationsAndFailsOnNewOnes(@TempDir Path store) {
    DcaRuleSelection frozen =
        DcaRuleSelection.all().frozen(ARCH_RULE).withFreezeStore(store.resolve("frozen"));

    assertEquals(
        DcaRuleOutcome.Status.PASSED,
        execute(BAD, ARCH_RULE, frozen).status(),
        "first run records the baseline");
    assertTrue(Files.isDirectory(store.resolve("frozen")), "the store was written");
    assertEquals(
        DcaRuleOutcome.Status.PASSED,
        execute(BAD, ARCH_RULE, frozen).status(),
        "the same violations stay accepted");

    DcaRuleSelection freshBaseline =
        DcaRuleSelection.all().frozen(ARCH_RULE).withFreezeStore(store.resolve("empty"));
    assertEquals(
        DcaRuleOutcome.Status.PASSED,
        execute(GOOD, ARCH_RULE, freshBaseline).status(),
        "a clean fixture records an empty baseline");
    DcaRuleOutcome newViolation = execute(BAD, ARCH_RULE, freshBaseline);
    assertEquals(
        DcaRuleOutcome.Status.FAILED,
        newViolation.status(),
        "a violation absent from the baseline fails: " + newViolation.message());
    assertTrue(newViolation.message().contains("PlaceOrderService"), newViolation.message());
  }

  /**
   * Tolerating one violation must never hide another: a rule that finds several has to collect them
   * all before the exceptions are applied. One fixture, four rules of different mechanics —
   * reflective checks, one ArchUnit rule per edge, two ArchUnit rules in sequence, one per context.
   */
  @Nested
  class IgnoringOneOfSeveralViolations {

    private static final String COLLECT = "dev.domaincentric.dca.archunit.fixtures.collect";

    private void assertOtherViolationRemains(String ruleId, String ignored, String remaining) {
      DcaRuleOutcome enforced = execute(COLLECT, ruleId, DcaRuleSelection.all());
      assertEquals(DcaRuleOutcome.Status.FAILED, enforced.status(), ruleId + " has violations");
      assertTrue(enforced.message().contains(ignored), enforced.message());
      assertTrue(enforced.message().contains(remaining), enforced.message());

      DcaRuleOutcome tolerated =
          execute(
              COLLECT,
              ruleId,
              DcaRuleSelection.all().ignoringViolationsMatching(ruleId, ".*" + ignored + ".*"));
      assertEquals(
          DcaRuleOutcome.Status.FAILED,
          tolerated.status(),
          ruleId + " must still fail on " + remaining + ": " + tolerated.message());
      assertFalse(tolerated.message().contains(ignored), tolerated.message());
      assertTrue(tolerated.message().contains(remaining), tolerated.message());
    }

    @Test
    void danglingUpstreams() {
      assertOtherViolationRemains("DCA-MAP-004", "missingOne", "missingTwo");
    }

    @Test
    void undeclaredCrossContextDependencies() {
      assertOtherViolationRemains("DCA-MAP-011", "ProductInfo", "PriceQuote");
    }

    @Test
    void transactionBoundariesOutsideTheApplicationLayer() {
      assertOtherViolationRemains("DCA-LAY-004", "CartController", "Cart");
    }

    @Test
    void sharedKernelDependingOnSeveralContexts() {
      assertOtherViolationRemains("DCA-STR-002", "ProductInfo", "PriceQuote");
    }
  }

  /** A regular expression is applied as written — a quantifier such as {@code {1,3}} included. */
  @Test
  void aToleratedPatternMayContainACommaQuantifier() {
    DcaRuleOutcome outcome =
        execute(
            BAD,
            ARCH_RULE,
            DcaRuleSelection.all().ignoringViolationsMatching(ARCH_RULE, "PlaceOrderServ.{1,3}"));

    assertEquals(DcaRuleOutcome.Status.PASSED, outcome.status(), outcome.message());
  }

  /**
   * Every line of a report carries the rule id, so that a reader can switch the rule off, a review
   * can cite it and an agent can map the line back to its catalog entry. The title cannot serve
   * that purpose because it changes with a configured suffix.
   */
  @Test
  void everyReportLineCarriesTheRuleId() {
    DcaRuleOutcome outcome = execute(BAD, ARCH_RULE, DcaRuleSelection.all());

    assertEquals(DcaRuleOutcome.Status.FAILED, outcome.status());
    assertEveryLineCarries(ARCH_RULE, outcome.message());
  }

  /** The same holds for a rule that collects its violations itself. */
  @Test
  void aSelfCollectingRuleAlsoCarriesTheRuleIdOnEveryLine() {
    DcaRuleOutcome outcome = execute(TACTICAL_BAD, CUSTOM_RULE, DcaRuleSelection.all());

    assertEquals(DcaRuleOutcome.Status.FAILED, outcome.status());
    assertEveryLineCarries(CUSTOM_RULE, outcome.message());
  }

  /** The id appears once per line, never twice, whatever the rule already wrote into its text. */
  @Test
  void theRuleIdIsNotRepeatedOnALineThatAlreadyCarriesIt() {
    DcaRuleOutcome outcome = execute(BAD, ARCH_RULE, DcaRuleSelection.all());

    for (String line : outcome.message().split("\n")) {
      assertFalse(
          line.indexOf("[" + ARCH_RULE + "]") != line.lastIndexOf("[" + ARCH_RULE + "]"), line);
    }
  }

  private static void assertEveryLineCarries(String ruleId, String message) {
    for (String line : message.split("\n")) {
      if (!line.isBlank()) {
        assertTrue(line.contains("[" + ruleId + "]"), "line without the rule id: " + line);
      }
    }
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

  /**
   * A rule that names its remedy gets it appended as one {@code Fix:} line, the counterpart of the
   * {@code fix} argument .NET's {@code DcaRule.Fail} has always taken. The line carries the rule id
   * like every other, so a grep for the id finds the advice with the finding.
   */
  @Test
  void aRuleWithARemedyAppendsOneFixLine() {
    DcaRuleOutcome outcome = execute(BAD, ARCH_RULE, DcaRuleSelection.all());

    assertEquals(DcaRuleOutcome.Status.FAILED, outcome.status());
    assertEquals(
        1,
        outcome.message().lines().filter(line -> line.contains("Fix: ")).count(),
        outcome.message());
    assertTrue(
        outcome.message().contains("[DCA-NAM-001] Fix: rename the class to *UseCase"),
        outcome.message());
  }

  /**
   * F34 of the 2026-09-21 review: the diagnostics wrote to {@code System.out}, where no report and
   * no pipeline stage sees them. What a diagnostic observes now travels in its outcome — it still
   * passes, because it asserts nothing, and it carries the rule id like every other line.
   */
  @Test
  void aDiagnosticPassesAndCarriesWhatItObserved() {
    DcaRule diagnostic =
        DcaRule.informational(
                "DCA-NAM-002",
                "Diagnostic: observed something",
                "a diagnostic asserts nothing",
                architecture -> java.util.List.of("one.Thing: says so", "another.Thing: too"))
            .selecting("nothing")
            .checking("nothing");

    DcaRuleOutcome outcome =
        DcaRuleExecution.execute(diagnostic, Fixtures.arch(BAD), DcaRuleSelection.all());

    assertEquals(DcaRuleOutcome.Status.PASSED, outcome.status());
    assertEquals(
        2,
        outcome.message().lines().filter(line -> line.startsWith("[DCA-NAM-002]")).count(),
        outcome.message());
    assertTrue(outcome.message().contains("one.Thing: says so"), outcome.message());
  }

  /** A diagnostic that saw nothing says nothing — no empty report to read past. */
  @Test
  void aDiagnosticThatObservedNothingCarriesNoMessage() {
    DcaRule quiet =
        DcaRule.informational(
                "DCA-STR-010",
                "Diagnostic: quiet",
                "nothing to say",
                architecture -> java.util.List.of())
            .selecting("nothing")
            .checking("nothing");

    DcaRuleOutcome outcome =
        DcaRuleExecution.execute(quiet, Fixtures.arch(BAD), DcaRuleSelection.all());

    assertEquals(DcaRuleOutcome.Status.PASSED, outcome.status());
    assertNull(outcome.message());
  }

  /** A rule that names none is unchanged: no empty Fix line. */
  @Test
  void aRuleWithoutARemedyAppendsNothing() {
    DcaRuleOutcome outcome = execute(BAD, "DCA-NAM-011", DcaRuleSelection.all());

    assertEquals(DcaRuleOutcome.Status.FAILED, outcome.status());
    assertFalse(outcome.message().contains("Fix: "), outcome.message());
  }
}
