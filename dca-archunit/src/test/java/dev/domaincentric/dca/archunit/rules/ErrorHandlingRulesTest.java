package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Self-test of {@link ErrorHandlingRules} against the error-handling fixture tree. */
class ErrorHandlingRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".errors";

  @Test
  void idsAreSequential() {
    Fixtures.assertIdsAreSequential(new ErrorHandlingRules(DcaLayout.forBasePackage("x")), "ERR");
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(ErrorHandlingRules::new, FIXTURES + ".good");
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(
        ErrorHandlingRules::new,
        FIXTURES + ".bad",
        // informational diagnostics, never fail; DCA-ERR-007 observed in .errors.fieldtypes
        "DCA-ERR-006",
        "DCA-ERR-007");
  }

  @Test
  @DisplayName("DCA-ERR-001 reports the domain exception that extends the platform's own type")
  void domainExceptionWithoutTheBaseTypeIsReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-001").getMessage();

    assertTrue(message.contains("ReservationAlreadyConfirmedException"), message);
  }

  @Test
  @DisplayName("DCA-ERR-002 reports a use-case failure declared in an adapter")
  void anExceptionOutsideItsLayerIsReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-002").getMessage();

    assertTrue(message.contains("ReservationUnavailableException"), message);
    assertTrue(message.contains("application layer"), message);
  }

  @Test
  @DisplayName("DCA-ERR-003 reports the use-case failure that extends the platform's own type")
  void useCaseFailureWithoutTheBaseTypeIsReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-003").getMessage();

    assertTrue(message.contains("ReservationNotFoundException"), message);
  }

  @Test
  @DisplayName("DCA-ERR-004 reports the container stereotype on an exception")
  void metadataOnAnExceptionIsReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-004").getMessage();

    assertTrue(message.contains("ReservationOverbookedException"), message);
  }

  @Test
  @DisplayName("DCA-ERR-004 reports the annotation that fixes the protocol answer")
  void transportStatusOnAnExceptionIsReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-004").getMessage();

    assertTrue(message.contains("ReservationWindowClosedException"), message);
    assertTrue(message.contains("ResponseStatus"), message);
  }

  @Test
  @DisplayName("DCA-ERR-005 reports the transport word in an exception name")
  void transportVocabularyInAnExceptionNameIsReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-005").getMessage();

    assertTrue(message.contains("ReservationHttpResponseException"), message);
  }

  /**
   * One misplacement, one owner. A subtype of DomainException declared in an application package is
   * misplaced, not mis-based: DCA-ERR-002 reports it and says to move it to the domain, while
   * DCA-ERR-003's remedy would be to change its base type, turning a broken rule of the model into
   * a use-case failure. Both reported it until 0.5.0.
   */
  @Test
  @DisplayName("a domain failure in the application layer is reported by DCA-ERR-002 only")
  void oneOwnerForAMisplacedDomainFailure() {
    String residence = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-002").getMessage();
    assertTrue(residence.contains("ReservationRuleBrokenException"), residence);

    String baseType = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-003").getMessage();
    assertFalse(baseType.contains("ReservationRuleBrokenException"), baseType);
  }

  /** DCA-ERR-007 lists display data, raw input and objects on a failure. */
  @Test
  void failuresCarryingStringsOrAggregatesAreListed() {
    var observed = Fixtures.observed(FIXTURES + ".fieldtypes.bad", "DCA-ERR-007");

    assertTrue(
        observed.stream()
            .anyMatch(
                line -> line.contains("AccountOverdrawnException.accountName is a plain string")),
        observed.toString());
    assertTrue(
        observed.stream()
            .anyMatch(
                line ->
                    line.contains("AccountOverdrawnException.account is an aggregate or entity")),
        observed.toString());
    assertTrue(
        observed.stream()
            .anyMatch(
                line -> line.contains("AccountNotFoundException.rawAccountId is a plain string")),
        observed.toString());
    assertEquals(5, observed.size(), observed.toString());
  }

  /** DCA-ERR-007: a code held in a plain string is listed - the type does not tell it from text. */
  @Test
  void aCodeHeldInAPlainStringIsListed() {
    var observed = Fixtures.observed(FIXTURES + ".fieldtypes.bad", "DCA-ERR-007");

    assertTrue(
        observed.stream()
            .anyMatch(line -> line.contains("BranchMismatchException.left is a plain string")),
        observed.toString());
    assertTrue(
        observed.stream()
            .anyMatch(line -> line.contains("BranchMismatchException.right is a plain string")),
        observed.toString());
  }

  /**
   * Ids, values, numbers, an enum, a code typed as a value and a list of ids pass; the base type's
   * message is not a field.
   */
  @Test
  void failuresCarryingIdsAndFiguresAreNotListed() {
    assertEquals(
        java.util.List.of(), Fixtures.observed(FIXTURES + ".fieldtypes.good", "DCA-ERR-007"));
  }
}
