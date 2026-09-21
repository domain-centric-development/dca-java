package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
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
    assertEquals(17, set.rules().size());
    assertEquals(17, set.rules().stream().map(DcaRule::id).distinct().count());
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

  /**
   * The specification role selects, not only the name. Both reference samples name their
   * specifications after the predicate they express ({@code HasMinTotal}, {@code ActiveCart}) and
   * carry the marker through an intermediate interface; under name-only selection neither rule saw
   * them.
   */
  @Test
  @DisplayName("DCA-ADV-017 reports a specification that carries the marker without the suffix")
  void aSpecificationWithoutTheSuffixIsPlacedByItsRole() {
    String message = Fixtures.failure(BAD, "DCA-ADV-017").getMessage();

    assertTrue(message.contains("HasMinTotal"), message);
  }

  /** And the role decides metadata ownership, so DCA-ONI-003 does not also claim it. */
  @Test
  @DisplayName("DCA-ADV-018 owns the metadata of a specification without the suffix")
  void aSpecificationWithoutTheSuffixIsOwnedByItsRole() {
    String owned = Fixtures.failure(BAD, "DCA-ADV-018").getMessage();
    assertTrue(owned.contains("ActiveOrder"), owned);

    // Ownership is exclusive, so the container stereotype reaches DCA-ONI-003 nowhere.
    Fixtures.rule(BAD, "DCA-ONI-003").check(Fixtures.arch(BAD));
  }
}
