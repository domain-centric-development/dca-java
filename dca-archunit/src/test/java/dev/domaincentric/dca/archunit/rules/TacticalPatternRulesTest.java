package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class TacticalPatternRulesTest {

  private static final String GOOD = "dev.domaincentric.dca.archunit.fixtures.tactical.good";
  private static final String BAD = "dev.domaincentric.dca.archunit.fixtures.tactical.bad";

  /** Rules without a negative fixture, with the reason. */
  private static final Set<String> NO_NEGATIVE_FIXTURE = Set.of();

  static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  private static List<DcaRule> rules(String pkg) {
    return new TacticalPatternRules(DcaLayout.forBasePackage(pkg)).rules();
  }

  @Test
  void ruleSetHasStableShape() {
    TacticalPatternRules set = new TacticalPatternRules(DcaLayout.forBasePackage(GOOD));
    assertEquals("tactical", set.name());
    assertEquals(22, set.rules().size());
    for (int i = 0; i < set.rules().size(); i++) {
      assertEquals(String.format("DCA-TAC-%03d", i + 1), set.rules().get(i).id());
    }
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    DcaArchitecture arch = arch(GOOD);
    return rules(GOOD).stream()
        .map(rule -> DynamicTest.dynamicTest(rule.toString(), () -> rule.check(arch)));
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    DcaArchitecture arch = arch(BAD);
    return rules(BAD).stream()
        .filter(rule -> !NO_NEGATIVE_FIXTURE.contains(rule.id()))
        .map(
            rule ->
                DynamicTest.dynamicTest(
                    rule.toString(),
                    () -> {
                      AssertionError error =
                          assertThrows(AssertionError.class, () -> rule.check(arch));
                      assertTrue(
                          error.getMessage() != null && !error.getMessage().isBlank(),
                          "violation message must explain the rule");
                    }));
  }
}
