package dev.domaincentric.dca.archunit.rules;

import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class StrategicPatternRulesTest {

  private static final String GOOD = Fixtures.ROOT + ".strategic.good";
  private static final String BAD = Fixtures.ROOT + ".strategic.bad";

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
        "DCA-STR-010" // documentation-only rule (code-review pattern), never fails
        );
  }
}
