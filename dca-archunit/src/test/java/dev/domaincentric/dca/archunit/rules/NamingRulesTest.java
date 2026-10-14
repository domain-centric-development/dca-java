package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class NamingRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".naming";

  @Test
  void idsAreSequential() {
    Fixtures.assertIdsAreSequential(new NamingRules(DcaLayout.forBasePackage("x")), "NAM");
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(NamingRules::new, FIXTURES + ".good");
  }

  /** Every naming rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(
        NamingRules::new,
        FIXTURES + ".bad",
        "DCA-NAM-002",
        // informational diagnostic, never fails; observed in .naming.trigger
        "DCA-NAM-012",
        // negative fixture in .naming.datawrites
        "DCA-NAM-013");
  }

  /** DCA-NAM-002 lists unannotated use cases but, like DCA-NAM-001, never a record. */
  @Test
  void theDiagnosticExcludesRecords() {
    var observed =
        Fixtures.rule(FIXTURES + ".bad", "DCA-NAM-002").observe(Fixtures.arch(FIXTURES + ".bad"));

    assertTrue(
        observed.stream().anyMatch(line -> line.contains("CancelOrderUseCase")),
        observed.toString());
    assertFalse(
        observed.stream().anyMatch(line -> line.contains("ShipOrderUseCase")), observed.toString());
  }

  /** DCA-NAM-012 lists an operation named after its trigger, and only a whole trigger word. */
  @Test
  void theTriggerDiagnosticListsOperationsNamedAfterTheirTrigger() {
    var observed = Fixtures.observed(FIXTURES + ".trigger.bad", "DCA-NAM-012");

    assertTrue(
        observed.stream().anyMatch(line -> line.contains("ArchiveEntryOnExpiryInputPort")),
        observed.toString());
    assertTrue(
        observed.stream().anyMatch(line -> line.contains("RestoreEntryAfterLoginUseCase")),
        observed.toString());
    assertEquals(4, observed.size(), observed.toString());
  }

  @Test
  void theTriggerDiagnosticPassesOverEffectNamesAndWordsThatOnlyStartLikeATrigger() {
    assertEquals(List.of(), Fixtures.observed(FIXTURES + ".trigger.good", "DCA-NAM-012"));
  }

  /** DCA-NAM-013 reports use cases, input ports and published operations named after a write. */
  @Test
  void operationsNamedAfterADataWriteAreReported() {
    String message = Fixtures.violation(FIXTURES + ".datawrites.bad", "DCA-NAM-013").getMessage();

    assertTrue(message.contains("SetLimitInputPort"), message);
    assertTrue(message.contains("SetLimitUseCase"), message);
    assertTrue(message.contains("UpdateStatusUseCase"), message);
    assertTrue(message.contains("LimitsApi.setLimit"), message);
    assertTrue(message.contains("LimitsApi.updateStatus"), message);
    assertFalse(message.contains("currentLimit"), message);
  }

  @Test
  void operationsNamedAfterWhatTheChangeMeansPass() {
    Fixtures.passes(FIXTURES + ".datawrites.good", "DCA-NAM-013");
  }
}
