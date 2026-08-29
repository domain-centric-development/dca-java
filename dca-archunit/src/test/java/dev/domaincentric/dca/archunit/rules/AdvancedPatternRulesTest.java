package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class AdvancedPatternRulesTest {

  private static final String GOOD = "dev.domaincentric.dca.archunit.fixtures.advanced.good";
  private static final String BAD = "dev.domaincentric.dca.archunit.fixtures.advanced.bad";

  /** Every rule of this set has a negative fixture. */
  private static final Set<String> NO_NEGATIVE_FIXTURE = Set.of();

  static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  static AdvancedPatternRules rules(String pkg) {
    return new AdvancedPatternRules(DcaLayout.forBasePackage(pkg));
  }

  @Test
  void setHasExpectedShape() {
    AdvancedPatternRules set = rules(GOOD);
    assertTrue(set.name().equals("advanced"));
    assertTrue(set.rules().size() == 18);
    assertTrue(set.rules().stream().map(DcaRule::id).distinct().count() == 18);
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    DcaArchitecture arch = arch(GOOD);
    return rules(GOOD).rules().stream()
        .map(rule -> DynamicTest.dynamicTest(rule.toString(), () -> rule.check(arch)));
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    DcaArchitecture arch = arch(BAD);
    return rules(BAD).rules().stream()
        .filter(rule -> !NO_NEGATIVE_FIXTURE.contains(rule.id()))
        .map(
            rule ->
                DynamicTest.dynamicTest(
                    rule.toString(),
                    () -> assertThrows(AssertionError.class, () -> rule.check(arch))));
  }
}
