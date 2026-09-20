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
        "DCA-STR-011" // the bad fixture declares its contexts; absence has its own fixture below
        );
  }

  @Test
  void codeBaseWithoutAnyDeclaredContextFails() {
    DcaRuleViolation violation = Fixtures.violation(UNDECLARED, "DCA-STR-011");

    String message = violation.getMessage();
    assertTrue(message.contains("@BoundedContext"), message);
    assertTrue(message.contains("passes without having looked at anything"), message);
  }
}
