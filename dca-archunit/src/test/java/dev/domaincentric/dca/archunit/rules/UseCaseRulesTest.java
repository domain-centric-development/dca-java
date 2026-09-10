package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class UseCaseRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".usecase";
  private static final String TRANSACTIONS = Fixtures.ROOT + ".transactions";

  @Test
  void idsAreSequential() {
    Fixtures.assertIdsAreSequential(new UseCaseRules(DcaLayout.forBasePackage("x")), "USE");
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(UseCaseRules::new, FIXTURES + ".good");
  }

  /** Every use-case rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(UseCaseRules::new, FIXTURES + ".bad");
  }

  @Nested
  @DisplayName("DCA-USE-015 — results carry no identities")
  class ResultShape {

    private List<String> violations() {
      return Fixtures.violation(FIXTURES + ".bad", "DCA-USE-015").violations();
    }

    @Test
    @DisplayName(
        "names every path to an identity: generic arguments, nested and shared part records")
    void namesEveryPathToAnIdentity() {
      List<String> violations = violations();
      assertTrue(
          violations.contains("ListOrdersResult.orders : Order (AggregateRoot)"),
          violations.toString());
      assertTrue(
          violations.contains(
              "ListOrdersResult.highlight -> Highlight.order : Order (AggregateRoot)"),
          violations.toString());
      assertTrue(
          violations.contains(
              "ListOrdersResult.firstLine -> OrderLine.item : OrderLineItem (Entity)"),
          violations.toString());
      assertTrue(
          violations.contains("ListOrdersResult.parts -> OrderPart.item : OrderLineItem (Entity)"),
          "a part record shared in application.shared is walked: " + violations);
      assertTrue(
          violations.stream().noneMatch(v -> v.startsWith("PlaceOrderResult")),
          "a result of values is not reported: " + violations);
    }

    /** The same part record reached through two fields is reported on both paths. */
    @Test
    @DisplayName("reports every path through a part record, not only the first")
    void reportsEveryPathThroughTheSamePartRecord() {
      List<String> violations = violations();
      assertTrue(
          violations.contains(
              "ListOrdersResult.lastLine -> OrderLine.item : OrderLineItem (Entity)"),
          violations.toString());
    }

    /** An instance field inherited from a base class without the suffix is part of the result. */
    @Test
    @DisplayName("includes inherited instance fields")
    void includesInheritedFields() {
      List<String> violations = violations();
      assertTrue(
          violations.contains("ArchivedOrdersResult.pinned : Order (AggregateRoot)"),
          violations.toString());
    }

    /**
     * {@code GenericBase<T>} declares {@code T value}; the result binds {@code T}. The inherited
     * field is read in the subclass's context, through every level of the hierarchy and inside
     * containers bound to the parameter.
     */
    @Test
    @DisplayName("resolves inherited generic fields in the result's context")
    void resolvesInheritedGenericFields() {
      List<String> violations = violations();
      assertTrue(
          violations.contains("GenericOrderResult.value : Order (AggregateRoot)"),
          "T = Order: " + violations);
      assertTrue(
          violations.contains("BatchedOrdersResult.value : Order (AggregateRoot)"),
          "T = List<U>, U = Order: " + violations);
      assertTrue(
          violations.contains("OrdersByRegionResult.value : Order (AggregateRoot)"),
          "T = Map<String, List<Order>>: " + violations);
      assertTrue(
          violations.contains("LineItemResult.value : OrderLineItem (Entity)"),
          "T = an entity: " + violations);
    }

    /** A type parameter alone exposes nothing; only fields do. */
    @Test
    @DisplayName("reports a bound type parameter only through a field that uses it")
    void reportsBoundParametersOnlyThroughFields() {
      List<String> violations = violations();
      assertTrue(
          violations.stream().noneMatch(v -> v.contains("count")),
          "the int field next to T is not reported: " + violations);
      // the good fixture's OrderCountResult extends UnusedParameterBase<Order> and the results
      // binding T to String, OrderId and Money all pass - see goodFixturePasses
    }
  }

  /**
   * Transaction placement is checked per method, following calls within the class: the method that
   * publishes must be transactional itself, or be reached from one that is (or that draws a
   * boundary). Whether a call sits inside the block handed to {@code inTransaction} is beyond
   * bytecode analysis — ArchUnit attributes a lambda's calls to the enclosing method — and is
   * documented as the remaining limit.
   */
  @Nested
  @DisplayName("DCA-USE-009 / DCA-USE-012 — transaction placement per method")
  class TransactionPlacement {

    @Test
    @DisplayName("a transactional executing method may delegate save and publish to a helper")
    void annotatedMethodReachingAHelperPasses() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertFalse(message.contains("AnnotatedExecuteUseCase"), message);
      String saves = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertFalse(saves.contains("AnnotatedExecuteUseCase"), saves);
    }

    @Test
    @DisplayName("a boundary drawn in the publishing method passes")
    void boundaryAroundPublicationPasses() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertFalse(message.contains("BoundaryAroundUseCase"), message);
    }

    @Test
    @DisplayName("an annotation on an unrelated method does not cover the publishing method")
    void unrelatedAnnotatedMethodDoesNotCount() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertTrue(message.contains("UnrelatedAnnotationUseCase"), message);
      assertTrue(message.contains("execute"), "names the publishing method: " + message);
    }

    @Test
    @DisplayName("a boundary in another method does not cover the publishing method")
    void boundaryElsewhereDoesNotCount() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertTrue(message.contains("BoundaryElsewhereUseCase"), message);
      assertTrue(message.contains("notifyLater"), "names the publishing method: " + message);
    }

    @Test
    @DisplayName("a save or delete without a boundary is reported even when nothing is published")
    void saveOrDeleteWithoutBoundaryIsReported() {
      String tx = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertTrue(tx.contains("SaveNoBoundaryUseCase.execute saves an aggregate without"), tx);
      assertTrue(tx.contains("SaveNoBoundaryUseCase.remove deletes an aggregate without"), tx);
      assertFalse(
          tx.contains("SaveWithoutPublishUseCase"),
          "a class-level annotation covers the save that publishes nothing: " + tx);
    }

    @Test
    @DisplayName("a publication in an unconnected method does not cover the saving method")
    void publicationInUnconnectedMethodDoesNotCount() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertTrue(message.contains("SaveWithoutPublishUseCase"), message);
      assertTrue(message.contains("execute"), "names the saving method: " + message);
    }

    @Test
    @DisplayName("publish(event) per event plus clearDomainEvents() is not a publication")
    void publishLoopIsReported() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertTrue(message.contains("PublishLoopUseCase"), message);
    }

    @Test
    @DisplayName("a helper two entry methods share does not connect their execution paths")
    void sharedHelperDoesNotConnectEntryMethods() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertTrue(message.contains("SharedHelperUseCase.execute"), message);
    }

    @Test
    @DisplayName("an entry method may save through one helper and publish through another")
    void splitHelpersPass() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertFalse(message.contains("SplitHelpersUseCase"), message);
      String tx = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertFalse(tx.contains("SplitHelpersUseCase"), tx);
    }

    @Test
    @DisplayName("delegation over several steps is followed")
    void multiStepDelegationPasses() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertFalse(message.contains("MultiStepUseCase"), message);
      String tx = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertFalse(tx.contains("MultiStepUseCase"), tx);
    }

    @Test
    @DisplayName("a shared saving helper is judged per entry path")
    void sharedSaveHelperIsJudgedPerEntryPath() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertTrue(message.contains("SharedSaveHelperUseCase.executeQuietly"), message);
      assertFalse(
          message.contains("SharedSaveHelperUseCase.execute "),
          "the publishing entry path is not reported: " + message);
    }

    @Test
    @DisplayName("recursive and mutually recursive helpers terminate")
    void recursionTerminates() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertFalse(message.contains("RecursiveSaveUseCase"), message);
      assertTrue(message.contains("MutualRecursionUseCase.ping"), message);
    }

    @Test
    @DisplayName("a public method stays an entry point when another method calls it")
    void publicMethodCalledInternallyIsStillAnEntryPoint() {
      String saves = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertTrue(saves.contains("DirectEntryUseCase.execute"), saves);
      assertFalse(saves.contains("DirectEntryUseCase.complete"), saves);
      String tx = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertTrue(tx.contains("PublicWrapperUseCase.execute"), tx);
      assertFalse(tx.contains("PublicWrapperUseCase.complete"), tx);
    }

    @Test
    @DisplayName("a boundary on one route to the publisher does not cover another route")
    void mixedDiamondIsReportedCoveredDiamondPasses() {
      String tx = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertTrue(tx.contains("MixedDiamondUseCase.execute"), tx);
      assertFalse(tx.contains("CoveredDiamondUseCase"), tx);
      String saves = Fixtures.failure(TRANSACTIONS, "DCA-USE-009").getMessage();
      assertFalse(saves.contains("DiamondUseCase"), saves);
    }

    @Test
    @DisplayName("a shared publishing helper is transactional per entry path")
    void sharedPublishHelperIsJudgedPerEntryPath() {
      String message = Fixtures.failure(TRANSACTIONS, "DCA-USE-012").getMessage();
      assertTrue(message.contains("SharedPublishHelperUseCase.executeQuietly"), message);
      assertFalse(
          message.contains("SharedPublishHelperUseCase.execute "),
          "the annotated entry path is not reported: " + message);
    }
  }
}
