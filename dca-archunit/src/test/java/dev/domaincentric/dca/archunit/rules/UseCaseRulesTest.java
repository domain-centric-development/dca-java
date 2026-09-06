package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class UseCaseRulesTest {

  private static final String FIXTURES = "dev.domaincentric.dca.archunit.fixtures.usecase";

  /** Rules without a negative fixture, with the reason. */
  private static final Set<String> NO_NEGATIVE_FIXTURE = Set.of();

  static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    DcaArchitecture good = arch(FIXTURES + ".good");
    return new UseCaseRules(good.layout())
        .rules().stream()
            .map(
                rule ->
                    DynamicTest.dynamicTest(
                        rule.toString(), () -> assertDoesNotThrow(() -> rule.check(good))));
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    DcaArchitecture bad = arch(FIXTURES + ".bad");
    return new UseCaseRules(bad.layout())
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
    java.util.List<DcaRule> rules = new UseCaseRules(DcaLayout.forBasePackage("x")).rules();
    return Stream.of(
        DynamicTest.dynamicTest(
            "ids numbered from 001 in order",
            () -> {
              for (int i = 0; i < rules.size(); i++) {
                String expected =
                    String.format("DCA-%s-%03d", "UseCase".equals("Naming") ? "NAM" : "USE", i + 1);
                if (!rules.get(i).id().equals(expected)) {
                  throw new AssertionError(rules.get(i).id() + " != " + expected);
                }
              }
            }));
  }

  /** DCA-USE-015 walks generic arguments, nested part records and same-package part records. */
  @Test
  void resultRuleNamesEveryPathToAnIdentity() {
    DcaArchitecture bad = arch(FIXTURES + ".bad");
    DcaRuleViolation violation =
        assertThrows(
            DcaRuleViolation.class,
            () -> UseCaseRules.resultsMustNotExposeAggregatesOrEntities(bad.layout()).check(bad));
    assertTrue(
        violation.violations().contains("ListOrdersResult.orders : Order (AggregateRoot)"),
        violation.toString());
    assertTrue(
        violation
            .violations()
            .contains("ListOrdersResult.highlight -> Highlight.order : Order (AggregateRoot)"),
        violation.toString());
    assertTrue(
        violation
            .violations()
            .contains("ListOrdersResult.firstLine -> OrderLine.item : OrderLineItem (Entity)"),
        violation.toString());
    assertTrue(
        violation
            .violations()
            .contains("ListOrdersResult.parts -> OrderPart.item : OrderLineItem (Entity)"),
        "a part record shared in application.shared is walked: " + violation);
    assertTrue(
        violation.violations().stream().noneMatch(v -> v.startsWith("PlaceOrderResult")),
        "a result of values is not reported: " + violation);
  }
}
