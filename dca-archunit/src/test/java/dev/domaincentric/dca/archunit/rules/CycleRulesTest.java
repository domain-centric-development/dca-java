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

  /**
   * One rule since 0.5.0: DCA-CYC-001 to DCA-CYC-004 sliced per module root, so the only cycle they
   * could report ran between two modules — and its first direction is already forbidden outright by
   * DCA-STR-004, DCA-STR-003, DCA-STR-006 and DCA-HEX-007. DCA-CYC-005 slices within a module and
   * is the one that reports something no sibling does.
   */
  @Test
  void setHasOneRule() {
    CycleRules set = new CycleRules(DcaLayout.forBasePackage(GOOD));
    assertEquals("cycles", set.name());
    assertEquals(1, set.rules().size());
    assertEquals("DCA-CYC-005", set.rules().get(0).id());
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(CycleRules::new, GOOD);
  }

  /** The remaining cycle rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(CycleRules::new, BAD);
  }
}
