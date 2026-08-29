package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

class StrategicPatternRulesTest {

  private static final String GOOD = "dev.domaincentric.dca.archunit.fixtures.strategic.good";
  private static final String BAD = "dev.domaincentric.dca.archunit.fixtures.strategic.bad";

  /** Rules without a negative fixture, with the reason. */
  private static final Set<String> NO_NEGATIVE_FIXTURE =
      Set.of(
          "DCA-STR-001", // diagnostic — prints the discovered contexts, never fails
          "DCA-STR-010" // documentation-only rule (code-review pattern), never fails
          );

  static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    DcaArchitecture arch = arch(GOOD);
    return new StrategicPatternRules(arch.layout())
        .rules().stream()
            .map(
                rule ->
                    DynamicTest.dynamicTest(
                        rule.id() + " passes on good fixture",
                        () -> assertDoesNotThrow(() -> rule.check(arch))));
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    DcaArchitecture arch = arch(BAD);
    return new StrategicPatternRules(arch.layout())
        .rules().stream()
            .filter(rule -> !NO_NEGATIVE_FIXTURE.contains(rule.id()))
            .map(
                (DcaRule rule) ->
                    DynamicTest.dynamicTest(
                        rule.id() + " fails on bad fixture",
                        () -> assertThrows(AssertionError.class, () -> rule.check(arch))));
  }
}
