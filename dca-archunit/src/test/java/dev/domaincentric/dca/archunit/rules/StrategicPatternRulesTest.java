package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class StrategicPatternRulesTest {

  private static final String GOOD = Fixtures.ROOT + ".strategic.good";
  private static final String BAD = Fixtures.ROOT + ".strategic.bad";
  private static final String UNDECLARED = Fixtures.ROOT + ".strategic.undeclared";

  /** No class exists below this package: a project that holds no production code yet. */
  private static final String NO_PRODUCTION_CODE = Fixtures.ROOT + ".noproductioncode";

  /** Only the class a generator writes to start the program. */
  private static final String ENTRY_POINT_ONLY = Fixtures.ROOT + ".entrypoint.only";

  /** The same entry point and one record beside it. */
  private static final String ENTRY_POINT_PLUS = Fixtures.ROOT + ".entrypoint.plus";

  /** A declared context whose layers carry names the layout does not know. */
  private static final String NO_MODULE_ROOT = Fixtures.ROOT + ".nomoduleroot";

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(StrategicPatternRules::new, GOOD);
  }

  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(
        StrategicPatternRules::new,
        BAD,
        "DCA-STR-001", // diagnostic — prints the discovered contexts, never fails
        "DCA-STR-010", // documentation-only rule (code-review pattern), never fails
        "DCA-STR-011", // the bad fixture declares its contexts; absence has its own fixture below
        "DCA-STR-012" // the bad fixture owns layers; their absence has its own fixture below
        );
  }

  @Test
  void codeBaseWithoutAnyDeclaredContextFails() {
    DcaRuleViolation violation = Fixtures.violation(UNDECLARED, "DCA-STR-011");

    String message = violation.getMessage();
    assertTrue(message.contains("@BoundedContext"), message);
    assertTrue(message.contains("passes without having looked at anything"), message);
  }

  /**
   * The third guard of the same kind as an empty import and an undeclared context: a layout that
   * discovers no module root leaves every layer-selecting rule with nothing to look at, and the
   * suite was green. This is the most likely first run of a brownfield code base.
   */
  @Test
  void codeBaseWhoseLayersTheLayoutDoesNotKnowFails() {
    DcaRuleViolation violation = Fixtures.violation(NO_MODULE_ROOT, "DCA-STR-012");

    String message = violation.getMessage();
    assertTrue(message.contains("no module root was discovered"), message);
    assertTrue(message.contains("withDomainSubpackage"), message);
    assertTrue(message.contains("passes without having looked at anything"), message);
  }

  /**
   * A project without production code has nothing to declare and no module to own a layer: both
   * guards pass, so the suite is green on an empty project. The two fixtures above keep them red
   * once code exists.
   */
  @Test
  void aProjectWithoutProductionCodePassesBothGuards() {
    assertTrue(Fixtures.arch(NO_PRODUCTION_CODE).classes().isEmpty());
    Fixtures.passes(NO_PRODUCTION_CODE, "DCA-STR-011");
    Fixtures.passes(NO_PRODUCTION_CODE, "DCA-STR-012");
  }

  /**
   * A freshly generated application: its only class starts the program. It holds no production code
   * yet, so both guards pass; one more real type and both fail.
   */
  @Test
  void aProjectWhoseOnlyClassStartsTheProgramPassesBothGuards() {
    Fixtures.passes(ENTRY_POINT_ONLY, "DCA-STR-011");
    Fixtures.passes(ENTRY_POINT_ONLY, "DCA-STR-012");
    Fixtures.violation(ENTRY_POINT_PLUS, "DCA-STR-011");
    Fixtures.violation(ENTRY_POINT_PLUS, "DCA-STR-012");
  }

  /** And a normal tree does not trip it. */
  @Test
  void aLayeredCodeBasePasses() {
    Fixtures.rule(GOOD, "DCA-STR-012").check(Fixtures.arch(GOOD));
  }
}
