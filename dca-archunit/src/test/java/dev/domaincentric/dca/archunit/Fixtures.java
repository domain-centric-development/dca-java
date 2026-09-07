package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;

/**
 * Test support shared by the rule-set tests: importing a fixture package as a {@link
 * DcaArchitecture}, looking a rule up by id, and the good/bad dynamic-test harness every rule set
 * runs against its fixture tree.
 */
public final class Fixtures {

  /** Root of every fixture package. */
  public static final String ROOT = "dev.domaincentric.dca.archunit.fixtures";

  private Fixtures() {}

  /** The fixture below {@code ROOT}, imported with the default layout for that package. */
  public static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  /** The catalog rule with the given id, built for the fixture's layout. */
  public static DcaRule rule(String pkg, String id) {
    return DcaRules.all(DcaLayout.forBasePackage(pkg)).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no such rule: " + id));
  }

  /** Runs the rule against the fixture and returns its collected violations. */
  public static DcaRuleViolation violation(String pkg, String id) {
    DcaArchitecture arch = arch(pkg);
    return assertThrows(DcaRuleViolation.class, () -> rule(pkg, id).check(arch), id);
  }

  /** Runs the rule against the fixture and returns whatever assertion it raised. */
  public static AssertionError failure(String pkg, String id) {
    DcaArchitecture arch = arch(pkg);
    return assertThrows(AssertionError.class, () -> rule(pkg, id).check(arch), id);
  }

  /** One dynamic test per rule of the set, each expecting the good fixture to pass. */
  public static Stream<DynamicTest> goodFixturePasses(
      Function<DcaLayout, DcaRuleSet> set, String goodPackage) {
    DcaArchitecture arch = arch(goodPackage);
    return set.apply(arch.layout()).rules().stream()
        .map(rule -> DynamicTest.dynamicTest(rule.toString(), () -> rule.check(arch)));
  }

  /**
   * One dynamic test per rule of the set, each expecting the bad fixture to fail with a message,
   * except for the rules named in {@code withoutNegativeFixture} (with their reason at the call
   * site).
   */
  public static Stream<DynamicTest> badFixtureFails(
      Function<DcaLayout, DcaRuleSet> set, String badPackage, String... withoutNegativeFixture) {
    DcaArchitecture arch = arch(badPackage);
    java.util.Set<String> excluded = java.util.Set.of(withoutNegativeFixture);
    return set.apply(arch.layout()).rules().stream()
        .filter(rule -> !excluded.contains(rule.id()))
        .map(
            rule ->
                DynamicTest.dynamicTest(
                    rule.toString(),
                    () -> {
                      AssertionError error =
                          assertThrows(AssertionError.class, () -> rule.check(arch));
                      if (error.getMessage() == null || error.getMessage().isBlank()) {
                        throw new AssertionError("violation message must explain the rule", error);
                      }
                    }));
  }

  /** The set's ids are {@code DCA-<PREFIX>-001}, {@code -002}, … in catalog order. */
  public static void assertIdsAreSequential(DcaRuleSet set, String prefix) {
    for (int i = 0; i < set.rules().size(); i++) {
      String expected = String.format("DCA-%s-%03d", prefix, i + 1);
      if (!set.rules().get(i).id().equals(expected)) {
        throw new AssertionError(set.rules().get(i).id() + " != " + expected);
      }
    }
  }
}
