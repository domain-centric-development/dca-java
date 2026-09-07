package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
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

  @Test
  void ruleSetHasStableShape() {
    TacticalPatternRules set = new TacticalPatternRules(DcaLayout.forBasePackage(GOOD));
    assertEquals("tactical", set.name());
    assertEquals(22, set.rules().size());
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
   * A container of the aggregate's own type holds <em>other</em> instances of that aggregate. Only
   * the direct field of the own type (a self-reference) is tolerated; a container never is.
   */
  @Test
  @DisplayName("DCA-TAC-003 rejects containers of the own aggregate type, tolerates a direct field")
  void containersOfTheOwnAggregateTypeAreReported() {
    List<String> violations = Fixtures.violation(BAD, "DCA-TAC-003").violations();
    for (String container : List.of("children", "parent", "siblings", "byName", "tree")) {
      assertReported(violations, ".Category", "'" + container + "'", "containing", "Category");
    }
    assertTrue(
        violations.stream().noneMatch(v -> v.contains("'root'")),
        "the direct self-reference is tolerated: " + violations);
    assertReported(violations, ".Order", "'customer'", "of type", "Customer");
    // the good fixture's Category holds a Category parent and List<CategoryId> - see
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
}
