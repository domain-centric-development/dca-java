package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class StrategicPatternRulesTest {

  private static final String GOOD = Fixtures.ROOT + ".strategic.good";
  private static final String BAD = Fixtures.ROOT + ".strategic.bad";
  private static final String UNDECLARED = Fixtures.ROOT + ".strategic.undeclared";

  /** A declared context whose layers carry names the layout does not know. */
  private static final String NO_MODULE_ROOT = Fixtures.ROOT + ".nomoduleroot";

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(StrategicPatternRules::new, GOOD);
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(
        StrategicPatternRules::new,
        BAD,
        "DCA-STR-001", // diagnostic — prints the discovered contexts, never fails
        "DCA-STR-010", // documentation-only rule (code-review pattern), never fails
        "DCA-STR-011", // the bad fixture declares its contexts; absence has its own fixture below
        "DCA-STR-012" // the bad fixture owns layers; their absence has its own fixture below
        );
  }

  @Test
  void codeBaseWithoutAnyDeclaredContextFails() {
    DcaRuleViolation violation = Fixtures.violation(UNDECLARED, "DCA-STR-011");

    String message = violation.getMessage();
    assertTrue(message.contains("@BoundedContext"), message);
    assertTrue(message.contains("passes without having looked at anything"), message);
  }

  /**
   * The third guard of the same kind as an empty import and an undeclared context: a layout that
   * discovers no module root leaves every layer-selecting rule with nothing to look at, and the
   * suite was green. This is the most likely first run of a brownfield code base.
   */
  @Test
  void codeBaseWhoseLayersTheLayoutDoesNotKnowFails() {
    DcaRuleViolation violation = Fixtures.violation(NO_MODULE_ROOT, "DCA-STR-012");

    String message = violation.getMessage();
    assertTrue(message.contains("no module root was discovered"), message);
    assertTrue(message.contains("withDomainSubpackage"), message);
    assertTrue(message.contains("passes without having looked at anything"), message);
  }

  /** And a normal tree does not trip it. */
  @Test
  void aLayeredCodeBasePasses() {
    Fixtures.rule(GOOD, "DCA-STR-012").check(Fixtures.arch(GOOD));
  }
}
