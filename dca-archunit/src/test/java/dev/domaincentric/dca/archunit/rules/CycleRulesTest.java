package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

class CycleRulesTest {

  private static final String GOOD = "dev.domaincentric.dca.archunit.fixtures.cycles.good";
  private static final String BAD = "dev.domaincentric.dca.archunit.fixtures.cycles.bad";

  /** Every cycle rule has a negative fixture. */
  private static final Set<String> NO_NEGATIVE_FIXTURE = Set.of();

  static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  static List<DcaRule> rules(String pkg) {
    return new CycleRules(DcaLayout.forBasePackage(pkg)).rules();
  }

  @Test
  void setHasFiveRulesInCatalogOrder() {
    List<String> ids = rules(GOOD).stream().map(DcaRule::id).toList();
    assertEquals(
        List.of("DCA-CYC-001", "DCA-CYC-002", "DCA-CYC-003", "DCA-CYC-004", "DCA-CYC-005"), ids);
    assertEquals("cycles", new CycleRules(DcaLayout.forBasePackage(GOOD)).name());
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
                    () -> assertThrows(AssertionError.class, () -> rule.check(arch))));
  }
}
