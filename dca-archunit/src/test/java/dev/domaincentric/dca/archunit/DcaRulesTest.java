package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;

/** The convenience selections reject what would otherwise select or exclude nothing. */
class DcaRulesTest {

  private static final DcaLayout LAYOUT = DcaLayout.forBasePackage("com.example");

  /**
   * ArchUnit renders {@code "<title>, because <rationale>"}. The rationale is its own sentence in
   * rules.json and in the catalog, so it starts with a capital there — inside this one it has to
   * read "because a domain service …", not "because A domain service …".
   */
  @Test
  void theRationaleReadsAsPartOfTheArchUnitSentence() {
    DcaRule rule =
        DcaRule.of(
                "DCA-TAC-001",
                "A title",
                "A domain service exists for logic that spans several aggregates",
                architecture ->
                    com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
                        .should()
                        .bePublic()
                        .allowEmptyShould(true))
            .selecting("nothing")
            .checking("nothing");

    String description =
        rule.archRule(
                DcaArchitecture.of(
                    LAYOUT,
                    new com.tngtech.archunit.core.importer.ClassFileImporter()
                        .importPackages("com.example")))
            .orElseThrow()
            .getDescription();

    assertTrue(description.contains("because a domain service exists"), description);
  }

  /** An acronym keeps its capital: "because DTOs are …", not "because dTOs are …". */
  @Test
  void anAcronymKeepsItsCapital() {
    DcaRule rule =
        DcaRule.of(
                "DCA-TAC-002",
                "A title",
                "DTOs are mapped at the edge",
                architecture ->
                    com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes()
                        .should()
                        .bePublic()
                        .allowEmptyShould(true))
            .selecting("nothing")
            .checking("nothing");

    String description =
        rule.archRule(
                DcaArchitecture.of(
                    LAYOUT,
                    new com.tngtech.archunit.core.importer.ClassFileImporter()
                        .importPackages("com.example")))
            .orElseThrow()
            .getDescription();

    assertTrue(description.contains("because DTOs are"), description);
  }

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
