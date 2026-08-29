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

class NamingRulesTest {

  private static final String FIXTURES = "dev.domaincentric.dca.archunit.fixtures.naming";

  /** Rules without a negative fixture, with the reason. */
  private static final Set<String> NO_NEGATIVE_FIXTURE = Set.of();

  static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    DcaArchitecture good = arch(FIXTURES + ".good");
    return new NamingRules(good.layout())
        .rules().stream()
            .map(
                rule ->
                    DynamicTest.dynamicTest(
                        rule.toString(), () -> assertDoesNotThrow(() -> rule.check(good))));
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    DcaArchitecture bad = arch(FIXTURES + ".bad");
    return new NamingRules(bad.layout())
        .rules().stream()
            .filter(rule -> !NO_NEGATIVE_FIXTURE.contains(rule.id()))
            .map(
                rule ->
                    DynamicTest.dynamicTest(
                        rule.toString(),
                        () -> assertThrows(AssertionError.class, () -> rule.check(bad))));
  }

  @TestFactory
  Stream<DynamicTest> idsAreSequential() {
    java.util.List<DcaRule> rules = new NamingRules(DcaLayout.forBasePackage("x")).rules();
    return Stream.of(
        DynamicTest.dynamicTest(
            "ids numbered from 001 in order",
            () -> {
              for (int i = 0; i < rules.size(); i++) {
                String expected =
                    String.format("DCA-%s-%03d", "Naming".equals("Naming") ? "NAM" : "USE", i + 1);
                if (!rules.get(i).id().equals(expected)) {
                  throw new AssertionError(rules.get(i).id() + " != " + expected);
                }
              }
            }));
  }
}
