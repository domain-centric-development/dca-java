package dev.domaincentric.dca.archunit.rules;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class NamingRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".naming";

  @Test
  void idsAreSequential() {
    Fixtures.assertIdsAreSequential(new NamingRules(DcaLayout.forBasePackage("x")), "NAM");
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(NamingRules::new, FIXTURES + ".good");
  }

  /** Every naming rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(NamingRules::new, FIXTURES + ".bad", "DCA-NAM-002");
  }
}
