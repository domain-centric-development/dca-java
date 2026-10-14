package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Self-test of {@link HexagonalRules} against the shared hexagonal fixture tree. */
class HexagonalRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".hexagonal";
  private static final String INFRASTRUCTURE = Fixtures.ROOT + ".infrastructure";

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(HexagonalRules::new, FIXTURES + ".good");
  }

  /** Every hexagonal rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(
        HexagonalRules::new,
        FIXTURES + ".bad",
        // negative fixture in .hexagonal.getmapping
        "DCA-HEX-014",
        // informational diagnostic, never fails; observed in .hexagonal.enumnames
        "DCA-HEX-016");
  }

  /**
   * The infrastructure predicate is exact: the root infrastructure package itself counts, and so
   * does a module's own {@code infrastructure} package — a class in either is an implementation
   * detail, wherever below the module it is imported from.
   */
  @Test
  @DisplayName("DCA-HEX-004 sees a class directly in the global infrastructure package")
  void incomingAdapterDependingOnRootInfrastructureIsReported() {
    String message = Fixtures.failure(INFRASTRUCTURE, "DCA-HEX-004").getMessage();
    assertTrue(message.contains("CartController") && message.contains("Wiring"), message);
  }

  @Test
  void outgoingAdapterMayReuseOwnAndGlobalInfrastructure() {
    Fixtures.rule(INFRASTRUCTURE, "DCA-HEX-005").check(Fixtures.arch(INFRASTRUCTURE));
  }

  @Test
  @DisplayName("DCA-HEX-005 names the foreign infrastructure client and survives unrelated ignores")
  void outgoingAdapterDependingOnForeignInfrastructureIsReportedByName() {
    String bad = FIXTURES + ".bad";
    var violation = Fixtures.violation(bad, "DCA-HEX-005");
    assertTrue(
        violation.violations().stream()
            .anyMatch(
                v ->
                    v.contains("ForeignInfrastructureAdapter")
                        && v.contains("infrastructure.Client")),
        violation.getMessage());
    // An ignore pattern that matches nothing must not swallow the first (or any) violation.
    var rule = Fixtures.rule(bad, "DCA-HEX-005");
    var selection =
        dev.domaincentric.dca.archunit.DcaRuleSelection.all()
            .ignoringViolationsMatching(rule.id(), ".*Unrelated.*");
    var outcome =
        dev.domaincentric.dca.archunit.DcaRuleExecution.execute(
            rule, Fixtures.arch(bad), selection);
    assertTrue(
        outcome.status() == dev.domaincentric.dca.archunit.DcaRuleOutcome.Status.FAILED
            && outcome.toString().contains("ForeignInfrastructureAdapter"),
        outcome.toString());
  }

  /** DCA-HEX-014, directly and through a helper the handler calls. */
  @Test
  void aGetHandlerThatDrivesACommandIsReported() {
    String message = Fixtures.failure(FIXTURES + ".getmapping.bad", "DCA-HEX-014").getMessage();

    assertTrue(
        message.contains(
            "LedgerController.show handles @GetMapping and drives a command through"
                + " PostEntryInputPort.execute"),
        message);
    assertTrue(message.contains("LedgerController.quickEntry handles @GetMapping"), message);
  }

  /** DCA-HEX-014 reads the method attribute of a general mapping the preset names. */
  @Test
  void aGeneralMappingRestrictedToGetIsSelected() {
    String message = Fixtures.failure(FIXTURES + ".getmapping.bad", "DCA-HEX-014").getMessage();

    assertTrue(
        message.contains(
            "LedgerController.reverse handles @RequestMapping(method=GET) and drives a command"),
        message);
  }

  /** A GET handler that asks a query passes; a handler without the GET mapping is not selected. */
  @Test
  void aGetHandlerThatAsksAQueryPasses() {
    Fixtures.passes(FIXTURES + ".getmapping.good", "DCA-HEX-014");
  }

  /** DCA-HEX-016 lists name() and toString() on an enum of the inner layers. */
  @Test
  void anAdapterSpellingADomainEnumByItsMemberNameIsListed() {
    var observed = Fixtures.observed(FIXTURES + ".enumnames.bad", "DCA-HEX-016");

    assertTrue(
        observed.stream()
            .anyMatch(
                line -> line.contains("LedgerController.status(") && line.contains("(name())")),
        observed.toString());
    assertTrue(
        observed.stream()
            .anyMatch(
                line -> line.contains("LedgerController.label(") && line.contains("(toString())")),
        observed.toString());
    assertTrue(observed.size() == 2, observed.toString());
  }

  /** A code per value, and the name of an enum the adapter declares itself, are not listed. */
  @Test
  void anExplicitCodeAndAnAdapterEnumAreNotListed() {
    assertTrue(Fixtures.observed(FIXTURES + ".enumnames.good", "DCA-HEX-016").isEmpty());
  }
}
