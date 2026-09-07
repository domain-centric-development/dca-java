package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class AdvancedPatternRulesTest {

  private static final String GOOD = Fixtures.ROOT + ".advanced.good";
  private static final String BAD = Fixtures.ROOT + ".advanced.bad";

  @Test
  void setHasExpectedShape() {
    AdvancedPatternRules set = new AdvancedPatternRules(DcaLayout.forBasePackage(GOOD));
    assertEquals("advanced", set.name());
    assertEquals(18, set.rules().size());
    assertEquals(18, set.rules().stream().map(DcaRule::id).distinct().count());
    Fixtures.assertIdsAreSequential(set, "ADV");
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(AdvancedPatternRules::new, GOOD);
  }

  /** Every rule of this set has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(AdvancedPatternRules::new, BAD);
  }
}
