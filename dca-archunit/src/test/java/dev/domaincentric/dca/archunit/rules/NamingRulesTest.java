package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.Fixtures;
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
    return Fixtures.badFixtureFails(NamingRules::new, FIXTURES + ".bad", "DCA-NAM-002");
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
}
