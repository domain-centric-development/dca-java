package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class CycleRulesTest {

  private static final String GOOD = Fixtures.ROOT + ".cycles.good";
  private static final String BAD = Fixtures.ROOT + ".cycles.bad";

  @Test
  void setHasFiveRulesInCatalogOrder() {
    CycleRules set = new CycleRules(DcaLayout.forBasePackage(GOOD));
    assertEquals("cycles", set.name());
    assertEquals(5, set.rules().size());
    Fixtures.assertIdsAreSequential(set, "CYC");
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(CycleRules::new, GOOD);
  }

  /** Every cycle rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(CycleRules::new, BAD);
  }
}
