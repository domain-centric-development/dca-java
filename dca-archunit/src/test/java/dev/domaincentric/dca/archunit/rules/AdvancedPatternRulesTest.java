package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaMarkers;
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
    assertEquals(16, set.rules().size());
    assertEquals(16, set.rules().stream().map(DcaRule::id).distinct().count());
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

  /**
   * DCA-ADV-008 asks that the occurrence time is <em>stored</em>, not computed — the marker already
   * forces the accessor. Which type stores it is the project's decision: a code base whose own
   * event vocabulary wraps the timestamp in a value object names that type instead of excluding the
   * rule.
   */
  @Test
  @DisplayName("DCA-ADV-008 follows the configured timestamp types")
  void aTimestampValueObjectSatisfiesTheRuleOnceItIsConfigured() {
    String own = Fixtures.ROOT + ".owntimestamp";
    DcaMarkers markers =
        DcaMarkers.dca().named("own").withDomainEvent(own + ".vocabulary.Happening");
    DcaLayout defaults = DcaLayout.forBasePackage(own).withMarkers(markers);

    AssertionError reported =
        assertThrows(
            AssertionError.class,
            () ->
                new AdvancedPatternRules(defaults)
                    .rules().stream()
                        .filter(r -> r.id().equals("DCA-ADV-008"))
                        .findFirst()
                        .orElseThrow()
                        .check(
                            DcaArchitecture.of(
                                defaults, new ClassFileImporter().importPackages(own))));
    assertTrue(reported.getMessage().contains("TariffBilled"), reported.getMessage());

    DcaLayout configured = defaults.withTimestampTypes(own + ".vocabulary.Timestamp");
    new AdvancedPatternRules(configured)
        .rules().stream()
            .filter(r -> r.id().equals("DCA-ADV-008"))
            .findFirst()
            .orElseThrow()
            .check(DcaArchitecture.of(configured, new ClassFileImporter().importPackages(own)));
  }
}
