package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Self-test of {@link LayeredRules} against the shared hexagonal fixture tree. */
class LayeredRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".hexagonal";
  private static final String INFRASTRUCTURE = Fixtures.ROOT + ".infrastructure";
  private static final String TRANSACTIONS = Fixtures.ROOT + ".transactions";

  @Test
  @DisplayName(
      "DCA-LAY-004 sees programmatic boundaries: a transaction API in an incoming adapter, TransactionBoundary in the domain")
  void programmaticBoundariesOutsideTheApplicationLayerAreReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-LAY-004").getMessage();
    assertTrue(message.contains("SeedRunner"), message);
    assertTrue(message.contains("TransactionalSeeder"), message);
    assertTrue(message.contains("TransactionalPricing"), message);
    assertTrue(message.contains("OrderResource"), message);
  }

  @Test
  @DisplayName("DCA-LAY-004 accepts TransactionBoundary in a use case")
  void aUseCaseMayDrawTheBoundaryItself() {
    Fixtures.rule(TRANSACTIONS, "DCA-LAY-004").check(Fixtures.arch(TRANSACTIONS));
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(LayeredRules::new, FIXTURES + ".good");
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(
        LayeredRules::new,
        FIXTURES + ".bad",
        // documentation-only rule, never fails
        "DCA-LAY-001",
        // the building-blocks markers are library code, not part of the fixture import
        "DCA-LAY-005");
  }

  /**
   * Infrastructure is selected by exact package: the global {@code base.infrastructure} package
   * itself (not only its sub-packages), every module's own {@code infrastructure} package, and
   * never a package whose name merely starts with the segment.
   */
  @Test
  @DisplayName("DCA-LAY-003 reports the root and the per-module infrastructure package, no more")
  void useCaseDependingOnInfrastructureAtEitherLevelIsReported() {
    String message = Fixtures.failure(INFRASTRUCTURE, "DCA-LAY-003").getMessage();
    assertTrue(message.contains("GetCartUseCase"), message);
    assertTrue(message.contains(".infrastructure.Wiring"), "root package itself: " + message);
    assertTrue(message.contains("cart.infrastructure.CartWiring"), "module package: " + message);
    assertFalse(message.contains("NotInfrastructure"), "exact segment boundary: " + message);
  }

  @Test
  @DisplayName("DCA-LAY-002 sees a domain depending on its module's infrastructure")
  void domainDependingOnModuleInfrastructureIsReported() {
    String message = Fixtures.failure(INFRASTRUCTURE, "DCA-LAY-002").getMessage();
    assertTrue(message.contains("Cart") && message.contains("CartWiring"), message);
  }
}
