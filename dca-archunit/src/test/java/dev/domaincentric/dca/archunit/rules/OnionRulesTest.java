package dev.domaincentric.dca.archunit.rules;

import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Self-test of {@link OnionRules} against the shared hexagonal fixture tree. */
class OnionRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".hexagonal";

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(OnionRules::new, FIXTURES + ".good");
  }

  /** Every onion rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(OnionRules::new, FIXTURES + ".bad");
  }
}
