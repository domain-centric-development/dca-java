package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class ContextMapRulesTest {

  private static final String GOOD = Fixtures.ROOT + ".contextmap.good";
  private static final String BAD = Fixtures.ROOT + ".contextmap.bad";
  private static final String NESTED = Fixtures.ROOT + ".contextmap.nested";
  private static final String COLLECT = Fixtures.ROOT + ".collect";
  private static final String PLANNED = Fixtures.ROOT + ".contextmap.planned";
  private static final String PLANNED_MISPLACED = Fixtures.ROOT + ".contextmap.plannedmisplaced";
  private static final String NO_MODULE = Fixtures.ROOT + ".contextmap.nomodule";

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(ContextMapRules::new, GOOD);
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(
        ContextMapRules::new,
        BAD,
        "DCA-MAP-013" // diagnostic — prints the declared context map, never fails
        );
  }

  /**
   * A relationship is a statement of the context, so the package that carries it must be the
   * context root. A declaration on a nested package is reported — it would otherwise be silently
   * ignored by every other context-map rule and by the renderer.
   */
  @Test
  @DisplayName("DCA-MAP-001 reports a relationship declared on a package below the context root")
  void declarationOnNestedPackageIsReported() {
    DcaRuleViolation violation = Fixtures.violation(NESTED, "DCA-MAP-001");
    assertEquals(1, violation.violations().size(), violation.getMessage());
    assertTrue(
        violation.violations().get(0).contains(NESTED + ".cart.application.getcart"),
        violation.getMessage());
    assertTrue(violation.violations().get(0).contains("@Partnership"), violation.getMessage());
  }

  /** Every dangling upstream is reported, not only the first one encountered. */
  @Test
  @DisplayName("DCA-MAP-004 reports every dangling upstream")
  void everyDanglingUpstreamIsReported() {
    DcaRuleViolation violation = Fixtures.violation(COLLECT, "DCA-MAP-004");
    assertEquals(2, violation.violations().size(), violation.getMessage());
    assertTrue(violation.getMessage().contains("missingOne"), violation.getMessage());
    assertTrue(violation.getMessage().contains("missingTwo"), violation.getMessage());
  }

  /** Both undeclared cross-context dependencies land in one violation. */
  @Test
  @DisplayName("a PLANNED declaration without code passes MAP-007..011: nothing is demanded")
  void plannedDeclarationsWithoutCodeDemandNothing() {
    var arch = Fixtures.arch(PLANNED);
    for (String id :
        new String[] {"DCA-MAP-007", "DCA-MAP-008", "DCA-MAP-009", "DCA-MAP-010", "DCA-MAP-011"}) {
      Fixtures.rule(PLANNED, id).check(arch);
    }
  }

  @Test
  @DisplayName("a PLANNED declaration does not exempt existing code from placement")
  void plannedDeclarationsStillGovernPlacement() {
    Fixtures.rule(PLANNED_MISPLACED, "DCA-MAP-007").check(Fixtures.arch(PLANNED_MISPLACED));
    for (String id : new String[] {"DCA-MAP-008", "DCA-MAP-009", "DCA-MAP-010"}) {
      String message = Fixtures.violation(PLANNED_MISPLACED, id).getMessage();
      assertTrue(message.contains("Invoice"), id + ": " + message);
      assertFalse(message.contains("needs translation evidence"), id + ": " + message);
    }
  }

  @Test
  @DisplayName("DCA-MAP-006 reports a missing module declaration once, not every edge")
  void missingModuleDeclarationIsOneDiagnostic() {
    DcaRuleViolation violation = Fixtures.violation(NO_MODULE, "DCA-MAP-006");
    assertEquals(1, violation.violations().size(), violation.getMessage());
    String message = violation.violations().get(0);
    assertTrue(message.contains("module declaration missing on 'billing'"), message);
    assertTrue(message.contains("allowed dependencies unknown"), message);
    assertFalse(message.contains("must describe the same edges"), message);
  }

  @Test
  @DisplayName("DCA-MAP-011 reports every undeclared edge")
  void everyUndeclaredEdgeIsReported() {
    DcaRuleViolation violation = Fixtures.violation(COLLECT, "DCA-MAP-011");
    assertTrue(
        violation.violations().stream().anyMatch(v -> v.contains("catalog :: api")),
        violation.getMessage());
    assertTrue(
        violation.violations().stream().anyMatch(v -> v.contains("pricing :: api")),
        violation.getMessage());
  }
}
