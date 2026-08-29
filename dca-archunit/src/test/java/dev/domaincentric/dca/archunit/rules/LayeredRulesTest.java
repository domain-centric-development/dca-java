package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/** Self-test of {@link LayeredRules} against the shared hexagonal fixture tree. */
class LayeredRulesTest {

  private static final String FIXTURES = "dev.domaincentric.dca.archunit.fixtures.hexagonal";

  /** Rules without a negative fixture, with the reason. */
  private static final Set<String> NO_NEGATIVE_FIXTURE =
      Set.of(
          // documentation-only rule, never fails
          "DCA-LAY-001",
          // the building-blocks markers are library code, not part of the fixture import
          "DCA-LAY-005");

  static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  private static Stream<DcaRule> rules(String pkg) {
    return new LayeredRules(DcaLayout.forBasePackage(pkg)).rules().stream();
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    String pkg = FIXTURES + ".good";
    DcaArchitecture arch = arch(pkg);
    return rules(pkg).map(rule -> dynamicTest(rule.toString(), () -> rule.check(arch)));
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    String pkg = FIXTURES + ".bad";
    DcaArchitecture arch = arch(pkg);
    return rules(pkg)
        .filter(rule -> !NO_NEGATIVE_FIXTURE.contains(rule.id()))
        .map(
            rule ->
                dynamicTest(
                    rule.toString(),
                    () -> assertThrows(AssertionError.class, () -> rule.check(arch))));
  }
}
