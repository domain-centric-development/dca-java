package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** The convenience selections reject what would otherwise select or exclude nothing. */
class DcaRulesTest {

  private static final DcaLayout LAYOUT = DcaLayout.forBasePackage("com.example");

  @Test
  void onlyRejectsAnUnknownSetName() {
    IllegalArgumentException rejected =
        assertThrows(IllegalArgumentException.class, () -> DcaRules.only(LAYOUT, "cycle"));

    assertTrue(rejected.getMessage().contains("cycles"), rejected.getMessage());
  }

  @Test
  void onlySelectsTheNamedSets() {
    assertFalse(DcaRules.only(LAYOUT, "cycles").isEmpty());
  }

  @Test
  void allExceptRejectsAnUnknownRuleId() {
    IllegalArgumentException rejected =
        assertThrows(
            IllegalArgumentException.class, () -> DcaRules.allExcept(LAYOUT, Set.of("DCA-NAM-2")));

    assertTrue(rejected.getMessage().contains("Unknown rule id"), rejected.getMessage());
  }

  @Test
  void allExceptAcceptsARetiredRuleId() {
    String retired = DcaRules.retired().keySet().iterator().next();

    assertFalse(DcaRules.allExcept(LAYOUT, Set.of(retired)).isEmpty());
  }
}
