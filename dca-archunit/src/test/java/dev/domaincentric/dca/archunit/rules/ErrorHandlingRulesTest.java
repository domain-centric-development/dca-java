package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.Fixtures;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

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
        "DCA-ERR-007",
        "DCA-ERR-008");
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

  /** DCA-ERR-006 says once that it cannot see failures of a project's own types. */
  @Test
  void withoutAnyFailureMarkerTypeTheDiagnosticListsNoPackage() {
    var observed = Fixtures.observed(FIXTURES + ".ownfailures", "DCA-ERR-006");

    assertEquals(1, observed.size(), observed.toString());
    assertTrue(observed.get(0).contains("no class of the scan is assignable"), observed.toString());
    assertFalse(observed.get(0).contains("adapter.incoming.rest:"), observed.toString());
  }

  /** An exhaustive switch over a sealed use-case result translates the use case's refusals. */
  @Test
  void aSwitchOverTheCasesOfASealedResultCountsAsTranslation() {
    var observed = Fixtures.observed(FIXTURES + ".results", "DCA-ERR-006");

    assertTrue(
        observed.stream().noneMatch(line -> line.contains("adapter.incoming.page:")),
        observed.toString());
  }

  /**
   * A pattern-matching switch whose arms never touch the bound case: the import model sees no
   * reference to a case type, the class file's constant pool does. The library compiles for Java
   * 17, so the switch is compiled here, on a runtime that knows it.
   */
  @Test
  void aPatternSwitchThatTouchesNoCaseMemberCountsAsTranslation(@TempDir Path compiled)
      throws Exception {
    assumeTrue(Runtime.version().feature() >= 21, "pattern switch needs Java 21");
    String pkg = FIXTURES + ".results.lending.adapter.incoming.web";
    String app = FIXTURES + ".results.lending.application";
    String source =
        "package "
            + pkg
            + ";\n"
            + "public final class LendingWebController {\n"
            + "  private final "
            + app
            + ".lendcopy.LendCopyInputPort lendCopy;\n"
            + "  public LendingWebController("
            + app
            + ".lendcopy.LendCopyInputPort lendCopy) { this.lendCopy = lendCopy; }\n"
            + "  public String lend(String member, String copy) {\n"
            + "    return switch (lendCopy.execute(new "
            + app
            + ".lendcopy.LendCopyCommand(member, copy))) {\n"
            + "      case "
            + app
            + ".lendcopy.LendCopyResult.Lent lent -> \"lent\";\n"
            + "      case "
            + app
            + ".lendcopy.LendCopyResult.LimitReached refused -> \"limit\";\n"
            + "    };\n"
            + "  }\n"
            + "}\n";
    Path file = compiled.resolve("LendingWebController.java");
    Files.writeString(file, source);
    JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
    int status =
        javac.run(
            null,
            null,
            null,
            "--release",
            "21",
            "-cp",
            System.getProperty("java.class.path"),
            "-d",
            compiled.toString(),
            file.toString());
    assertEquals(0, status, "the switch fixture compiles");
    Files.delete(file);

    String root = FIXTURES + ".results";
    Set<Location> locations = new HashSet<>(Locations.ofPackage(root));
    locations.add(Location.of(compiled));
    DcaArchitecture arch =
        DcaArchitecture.of(
            DcaLayout.forBasePackage(root), new ClassFileImporter().importLocations(locations));
    var observed = Fixtures.rule(root, "DCA-ERR-006").observe(arch);

    assertTrue(
        arch.classes().stream()
            .anyMatch(type -> type.getSimpleName().equals("LendingWebController")),
        "the compiled switch is imported");
    assertTrue(
        observed.stream().noneMatch(line -> line.contains("adapter.incoming.web:")),
        observed.toString());
  }

  /** Neither a failure type nor a case of a closed result: the package is still listed. */
  @Test
  void anAdapterWithNeitherHandlerNorResultSwitchIsStillListed() {
    var observed = Fixtures.observed(FIXTURES + ".results", "DCA-ERR-006");

    assertTrue(
        observed.stream().anyMatch(line -> line.contains("adapter.incoming.rest:")),
        observed.toString());
  }

  /** Holding the closed result without looking at a case is no translation. */
  @Test
  void receivingASealedResultWithoutItsCasesIsStillListed() {
    var observed = Fixtures.observed(FIXTURES + ".results", "DCA-ERR-006");

    assertTrue(
        observed.stream().anyMatch(line -> line.contains("adapter.incoming.feed:")),
        observed.toString());
  }

  /** Where failure types derive from the markers, DCA-ERR-006 lists packages as before. */
  @Test
  void withFailureMarkerTypesTheDiagnosticListsPackages() {
    var observed = Fixtures.observed(FIXTURES + ".bad", "DCA-ERR-006");

    assertTrue(
        observed.stream().noneMatch(line -> line.contains("no class of the scan is assignable")),
        observed.toString());
  }

  @Test
  @DisplayName("DCA-ERR-008 lists a closed result whose refusing case is not marked")
  void aClosedResultWithoutARefusalCaseIsListed() {
    var observed = Fixtures.observed(FIXTURES + ".bad", "DCA-ERR-008");

    assertEquals(1, observed.size(), observed.toString());
    assertTrue(observed.get(0).contains("CancelReservationResult is answered by"), observed.get(0));
    assertTrue(observed.get(0).contains("[Cancelled, NotFound]"), observed.get(0));
    assertTrue(observed.get(0).contains("is none of these outcomes a refusal"), observed.get(0));
  }

  @Test
  @DisplayName("DCA-ERR-008 lists a closed result of two successes and does not fail")
  void aClosedResultOfSuccessesIsListedButDoesNotFail() {
    Fixtures.passes(FIXTURES + ".results", "DCA-ERR-008");
    var observed = Fixtures.observed(FIXTURES + ".results", "DCA-ERR-008");

    assertEquals(1, observed.size(), observed.toString());
    assertTrue(observed.get(0).contains("SendReminderResult"), observed.toString());
    assertTrue(observed.get(0).contains("[AlreadySent, Sent]"), observed.toString());
  }

  @Test
  @DisplayName("DCA-ERR-008 does not list a closed result with a marked refusal case")
  void aClosedResultWithARefusalCaseIsNotListed() {
    var observed = Fixtures.observed(FIXTURES + ".results", "DCA-ERR-008");

    assertTrue(
        observed.stream()
            .noneMatch(line -> line.contains("LendCopyResult") || line.contains("RenewLoanResult")),
        observed.toString());
  }

  @Test
  @DisplayName("DCA-ERR-008 counts refusals grouped below a sealed case")
  void aRefusalBelowASealedCaseCounts() {
    assertEquals(java.util.List.of(), Fixtures.observed(FIXTURES + ".good", "DCA-ERR-008"));
  }

  @Test
  @DisplayName("DCA-ERR-002 reports an output-port failure the adapter declares itself")
  void anOutputPortExceptionOutsideTheApplicationIsReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-002").getMessage();

    assertTrue(message.contains("StoreRefusedReservationException"), message);
  }

  @Test
  @DisplayName(
      "DCA-ERR-003 reports an application failure that extends InputPortException directly")
  void aDirectInputPortExceptionIsReported() {
    String message = Fixtures.failure(FIXTURES + ".bad", "DCA-ERR-003").getMessage();

    assertTrue(message.contains("ReservationQuotaExceededException"), message);
  }

  @Test
  @DisplayName("DCA-ERR-002/003 accept an output-port failure declared beside its port")
  void anOutputPortExceptionBesideItsPortPasses() {
    Fixtures.passes(FIXTURES + ".good", "DCA-ERR-002");
    Fixtures.passes(FIXTURES + ".good", "DCA-ERR-003");
  }

  @Test
  @DisplayName("DCA-ERR-006 lists an incoming adapter that catches the abstract port failure")
  void anAdapterCatchingThePortFailureGenericallyIsListed() {
    var observed = Fixtures.observed(FIXTURES + ".bad", "DCA-ERR-006");

    assertTrue(
        observed.stream()
            .anyMatch(
                line ->
                    line.contains(
                        "ReservationRestController: names the abstract base PortException")),
        observed.toString());
  }

  @Test
  @DisplayName("DCA-ERR-006 lists no generic catch where the adapters name the failures")
  void noGenericCatchInTheGoodFixture() {
    var observed = Fixtures.observed(FIXTURES + ".good", "DCA-ERR-006");

    assertTrue(
        observed.stream().noneMatch(line -> line.contains("names the abstract base")),
        observed.toString());
  }

  @Test
  @DisplayName("DCA-ERR-009 reports a use case that constructs a domain exception for a not-found")
  void aUseCaseConstructingADomainExceptionIsReported() {
    String message = Fixtures.violation(FIXTURES + ".bad", "DCA-ERR-009").getMessage();

    assertTrue(message.contains("CancelReservationUseCase"), message);
    assertTrue(
        message.contains(
            "constructs " + FIXTURES + ".bad.reservation.domain.model.ReservationUnknownException"),
        message);
    assertFalse(
        message.contains("ReservationOverbookedException("),
        "a constructor's call to its parent is no construction");
    assertFalse(message.contains("Reservation.confirm"), "the aggregate raises its own failure");
  }

  @Test
  @DisplayName(
      "DCA-ERR-009 holds for an aggregate and a domain service that raise a domain failure")
  void theDomainLayerConstructsItsOwnFailures() {
    Fixtures.passes(FIXTURES + ".good", "DCA-ERR-009");
  }
}
