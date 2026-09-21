package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.archunit.DcaRules;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

class TacticalPatternRulesTest {

  private static final String GOOD = Fixtures.ROOT + ".tactical.good";
  private static final String BAD = Fixtures.ROOT + ".tactical.bad";
  private static final String STATICS = Fixtures.ROOT + ".tactical.statics";

  @Test
  void ruleSetHasStableShape() {
    TacticalPatternRules set = new TacticalPatternRules(DcaLayout.forBasePackage(GOOD));
    assertEquals("tactical", set.name());
    assertEquals(21, set.rules().size());
    Fixtures.assertIdsAreSequential(set, "TAC");
  }

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(TacticalPatternRules::new, GOOD);
  }

  /** Every tactical rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(TacticalPatternRules::new, BAD);
  }

  @Test
  @DisplayName(
      "a static port breaks persistence ignorance (TAC-002), a static same-type field holds no aggregate (TAC-003)")
  void staticFieldsArePortsButNotAggregateState() {
    String message = Fixtures.violation(STATICS, "DCA-TAC-002").getMessage();
    assertTrue(message.contains("lookup"), message);
    Fixtures.rule(STATICS, "DCA-TAC-003").check(Fixtures.arch(STATICS));
  }

  @Test
  void markerInterfacesAndSuppliersCannotHideIdentities() {
    assertReported(
        Fixtures.violation(BAD, "DCA-TAC-003").violations(),
        ".Order",
        "linkedOrder",
        "OrderReference");
    assertReported(
        Fixtures.violation(BAD, "DCA-TAC-003").violations(), ".Order", "suppliedOrder", "Order");
    assertReported(
        Fixtures.violation(BAD, "DCA-TAC-007").violations(),
        ".Shipment",
        "linkedOrder",
        "OrderReference");
    assertReported(
        Fixtures.violation(BAD, "DCA-TAC-008").violations(),
        ".ReferenceValue",
        "order",
        "OrderReference");
  }

  private static void assertReported(List<String> violations, String owner, String... fragments) {
    assertTrue(
        violations.stream()
            .anyMatch(v -> v.contains(owner) && Stream.of(fragments).allMatch(f -> v.contains(f))),
        "expected a violation for "
            + owner
            + " mentioning "
            + List.of(fragments)
            + " in:\n"
            + String.join("\n", violations));
  }

  /**
   * A field's type is walked completely — map values, optionals, arrays and nested containers — not
   * only the first type argument of a {@code List}, {@code Set} or {@code Collection}.
   */
  @Test
  @DisplayName("DCA-TAC-008 finds identities in a map, an optional and a nested container")
  void valueObjectHidingIdentitiesInContainersIsReported() {
    List<String> violations = Fixtures.violation(BAD, "DCA-TAC-008").violations();
    assertReported(violations, "Ledger", "ordersBySku", "Order", "aggregate root");
    assertReported(violations, "Ledger", "lastShipment", "Shipment", "entity");
    assertReported(violations, "Ledger", "customers", "Customer", "aggregate root");
  }

  @Test
  @DisplayName("DCA-TAC-003 finds another aggregate root inside a nested container")
  void aggregateHidingAnotherAggregateInNestedContainerIsReported() {
    List<String> violations = Fixtures.violation(BAD, "DCA-TAC-003").violations();
    assertReported(violations, ".Order", "customersByRegion", "Customer");
  }

  /**
   * A container of the aggregate's own type holds <em>other</em> instances of that aggregate.
   * Direct references and containers are both rejected.
   */
  @Test
  @DisplayName("DCA-TAC-003 rejects containers of the own aggregate type, rejects a direct field")
  void containersOfTheOwnAggregateTypeAreReported() {
    List<String> violations = Fixtures.violation(BAD, "DCA-TAC-003").violations();
    for (String container : List.of("children", "parent", "siblings", "byName", "tree")) {
      assertReported(violations, ".Category", "'" + container + "'", "containing", "Category");
    }
    assertReported(violations, ".Category", "'root'", "of type", "Category");
    assertReported(violations, ".Order", "'customer'", "of type", "Customer");
    // the good fixture's Category holds a CategoryId parent and List<CategoryId> - see
    // goodFixturePasses
  }

  @Test
  @DisplayName("DCA-TAC-007 finds aggregate roots in an array")
  void entityHoldingAnArrayOfAggregatesIsReported() {
    List<String> violations = Fixtures.violation(BAD, "DCA-TAC-007").violations();
    assertReported(violations, "Shipment", "recipients", "Customer");
  }

  /**
   * {@code equals(Weight)} overloads instead of overriding: the class still compares by identity
   * through {@code Object.equals(Object)}. Only the exact signatures count.
   */
  @Test
  @DisplayName("DCA-TAC-012 does not mistake an equals overload for an override")
  void equalsOverloadIsNotAttributeEquality() {
    DcaRuleViolation violation = Fixtures.violation(BAD, "DCA-TAC-012");
    assertTrue(
        violation.violations().stream().anyMatch(v -> v.contains("Weight")),
        violation.getMessage());
  }

  /**
   * An enum value object with constant-specific class bodies is compiled abstract, so requiring
   * {@code final} of it could never be satisfied. DCA-TAC-009 therefore does not select enums - as
   * the .NET twin never has.
   */
  @Test
  @DisplayName("DCA-TAC-009 does not select an enum value object")
  void enumValueObjectsAreNotSelected() {
    Fixtures.rule(GOOD, "DCA-TAC-009").check(Fixtures.arch(GOOD));
  }

  /** The asynchronous spellings are the same names to DCA-TAC-021, as they are in .NET. */
  @Test
  @DisplayName("DCA-TAC-021 reports the asynchronous write vocabulary too")
  void asynchronousRepositorySemanticsOnAStoreAreReported() {
    DcaRuleViolation violation = Fixtures.violation(BAD, "DCA-TAC-021");
    assertTrue(
        violation.violations().stream().anyMatch(v -> v.contains("saveAsync")),
        violation.getMessage());
  }

  /**
   * The aggregate comes from the bound type argument, and the name must name it. A repository bound
   * to one aggregate and named after another is what the name-only resolution could not see.
   */
  @Test
  @DisplayName("DCA-TAC-016 reports a repository named after another aggregate than it binds")
  void aRepositoryMustBeNamedAfterTheAggregateItBinds() {
    DcaRuleViolation violation = Fixtures.violation(BAD, "DCA-TAC-016");

    assertTrue(
        violation.violations().stream()
            .anyMatch(
                v -> v.contains("CategoryRepository") && v.contains("name it OrderRepository")),
        violation.getMessage());
  }

  /**
   * A generic intermediate port binds a type variable, not an aggregate. Resolving its name would
   * look for a class called "Audited" and report a violation with no remedy, so it is skipped.
   */
  @Test
  @DisplayName("DCA-TAC-016 skips a generic intermediate repository port")
  void aGenericIntermediateRepositoryPortIsSkipped() {
    Fixtures.rule(GOOD, "DCA-TAC-016").check(Fixtures.arch(GOOD));
  }

  /**
   * The suffix drives the selection, so a project whose ports are called something else configures
   * the layout rather than excluding the ids. With the repository suffix changed, the bad fixture's
   * {@code *Repository} interfaces are no longer selected and the two name-anchored rules pass.
   */
  @Test
  @DisplayName("the tactical suffixes select: a changed repository suffix moves the selection")
  void theRepositorySuffixDrivesTheSelection() {
    DcaLayout own = DcaLayout.forBasePackage(BAD).withRepositorySuffix("Gateway");
    DcaArchitecture arch = DcaArchitecture.of(own, new ClassFileImporter().importPackages(BAD));

    for (String id : java.util.List.of("DCA-TAC-013", "DCA-TAC-016")) {
      DcaRules.all(own).stream()
          .filter(r -> r.id().equals(id))
          .findFirst()
          .orElseThrow()
          .check(arch);
    }

    // and with the default suffix they still report, so the test is not vacuous
    assertTrue(
        Fixtures.violation(BAD, "DCA-TAC-016").violations().stream()
            .anyMatch(v -> v.contains("Repository")),
        "the default suffix still selects");
  }
}
