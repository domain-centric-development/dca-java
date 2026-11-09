package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
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
  private static final String PERSISTENCE_API = Fixtures.ROOT + ".persistenceapi";

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
        // a diagnostic, lists and never fails
        "DCA-LAY-007",
        // the building-blocks markers are library code, not part of the fixture import
        "DCA-LAY-005",
        // negative fixture in fixtures.persistenceapi, see below
        "DCA-LAY-008");
  }

  @Test
  @DisplayName(
      "DCA-LAY-008 passes a use case behind its output port and one with the transactional"
          + " annotation")
  void aUseCaseBehindItsPortPasses() {
    Fixtures.passes(PERSISTENCE_API + ".good", "DCA-LAY-008");
  }

  @Test
  @DisplayName(
      "DCA-LAY-008 reports a persistence API and a programmatic transaction in a use case, not the"
          + " transactional annotation, not an outgoing adapter and not a library outside the list")
  void persistenceApisInTheApplicationLayerAreReported() {
    String message = Fixtures.failure(PERSISTENCE_API + ".bad", "DCA-LAY-008").getMessage();
    assertTrue(message.contains("RecordPostingUseCase"), message);
    assertTrue(message.contains("java.sql.Connection"), message);
    assertFalse(message.contains("ConnectionPostings"), "outgoing adapter: " + message);
    assertFalse(message.contains("ClosePeriodUseCase"), "unlisted library: " + message);
    assertTrue(message.contains("jakarta.transaction.UserTransaction"), "programmatic: " + message);
    assertFalse(
        message.contains("jakarta.transaction.Transactional"), "declarative passes: " + message);
  }

  @Test
  @DisplayName("DCA-LAY-008 exempts the annotation of the transactional role, and only that")
  void theDeclarativeExemptionFollowsTheTransactionalRole() {
    String pkg = PERSISTENCE_API + ".good";
    DcaLayout withoutRole = DcaLayout.forBasePackage(pkg).withFrameworkPreset("none");
    String message = assertThrows(AssertionError.class, () -> check(withoutRole, pkg)).getMessage();
    assertTrue(message.contains("CloseDayUseCase"), message);
    assertTrue(message.contains("jakarta.transaction.Transactional"), message);
  }

  @Test
  @DisplayName("DCA-LAY-008 follows the configured persistence API prefixes")
  void thePersistenceApiPrefixesAreConfigurable() {
    String pkg = PERSISTENCE_API + ".bad";
    DcaLayout own = DcaLayout.forBasePackage(pkg).withPersistenceApiPrefixes(pkg + ".vendor.");
    String message = assertThrows(AssertionError.class, () -> check(own, pkg)).getMessage();
    assertTrue(message.contains("ClosePeriodUseCase"), message);
    assertFalse(
        message.contains("RecordPostingUseCase"), "the default list is replaced: " + message);

    check(DcaLayout.forBasePackage(pkg).withPersistenceApiPrefixes(), pkg);
  }

  private static void check(DcaLayout layout, String pkg) {
    DcaRule rule =
        new LayeredRules(layout)
            .rules().stream().filter(r -> r.id().equals("DCA-LAY-008")).findFirst().orElseThrow();
    rule.check(DcaArchitecture.of(layout, new ClassFileImporter().importPackages(pkg)));
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
