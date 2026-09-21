package dev.domaincentric.dca.archunit.rules;

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
        // informational diagnostic, never fails
        "DCA-ERR-006");
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

    assertTrue(message.contains("ReservationResponseException"), message);
  }
}
