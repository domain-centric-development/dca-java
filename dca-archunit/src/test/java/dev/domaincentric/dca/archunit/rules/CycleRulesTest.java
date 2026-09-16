package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

  /**
   * The domain-model segment is read from the layout: with it renamed, the bad fixture's
   * domain.model packages are no longer sliced and the cycle between them is not seen.
   */
  @Test
  void theDomainModelSegmentComesFromTheLayout() {
    var arch = Fixtures.arch(BAD);
    assertThrows(
        AssertionError.class,
        () -> CycleRules.domainPackagesFreeOfCycles(arch.layout()).check(arch));
    var renamed =
        dev.domaincentric.dca.archunit.DcaArchitecture.of(
            arch.layout().withModelSubpackage("entities"), arch.classes());
    CycleRules.domainPackagesFreeOfCycles(renamed.layout()).check(renamed);
  }

  /** Every cycle rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(CycleRules::new, BAD);
  }
}
