package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rules select on the configured roles, not on the library's own types: a code base with its
 * own vocabulary is governed once the roles name its markers, and is invisible to the rules until
 * then.
 */
class DcaMarkersTest {

  private static final String FIXTURE = Fixtures.ROOT + ".ownmarkers";
  private static final String VOCABULARY = FIXTURE + ".vocabulary";

  /** The same vocabulary, in a tree whose domain failure sits in an incoming adapter. */
  private static final String MISPLACED = Fixtures.ROOT + ".ownmarkersbad";

  private static final DcaMarkers OWN =
      DcaMarkers.dca()
          .named("own")
          .withAggregateRoot(VOCABULARY + ".ContractRoot")
          .withRepository(VOCABULARY + ".ContractStore")
          .withDomainException(VOCABULARY + ".ContractFailure");

  private static DcaArchitecture arch(DcaMarkers markers) {
    return arch(FIXTURE, markers);
  }

  private static DcaArchitecture arch(String pkg, DcaMarkers markers) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg).withMarkers(markers),
        new ClassFileImporter().importPackages(pkg));
  }

  private static DcaRule rule(DcaMarkers markers, String id) {
    return rule(FIXTURE, markers, id);
  }

  private static DcaRule rule(String pkg, DcaMarkers markers, String id) {
    return DcaRules.all(DcaLayout.forBasePackage(pkg).withMarkers(markers)).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no such rule: " + id));
  }

  @Test
  @DisplayName("the default roles name the library's own markers")
  void theDefaultIsTheLibrarysVocabulary() {
    assertTrue(DcaMarkers.dca().isDefault());
    assertEquals("dca", DcaMarkers.dca().name());
    assertEquals(
        DcaMarkers.DCA_BUILDING_BLOCKS + ".hexagonal.port.out.Repository",
        DcaMarkers.dca().repository());
  }

  @Test
  @DisplayName("a code base with its own markers is invisible to the rules while the roles default")
  void ownVocabularyIsNotSelectedByTheDefaultRoles() {
    rule(DcaMarkers.dca(), "DCA-TAC-002").check(arch(DcaMarkers.dca()));
  }

  @Test
  @DisplayName("the same rule reports the same code once the roles name that vocabulary")
  void ownVocabularyIsGovernedOnceTheRolesNameIt() {
    DcaArchitecture arch = arch(OWN);

    AssertionError failure =
        assertThrows(AssertionError.class, () -> rule(OWN, "DCA-TAC-002").check(arch));

    assertTrue(failure.getMessage().contains("TariffContract"), failure.getMessage());
    assertTrue(failure.getMessage().contains("TariffContractStore"), failure.getMessage());
    assertFalse(OWN.isDefault());
  }

  @Test
  @DisplayName("the error rules follow the configured exception base type")
  void theErrorRulesFollowTheConfiguredBaseType() {
    DcaArchitecture arch = arch(OWN);

    rule(OWN, "DCA-ERR-001").check(arch);
    rule(OWN, "DCA-ERR-002").check(arch);
    rule(OWN, "DCA-ERR-003").check(arch);
    rule(OWN, "DCA-ERR-004").check(arch);
    rule(OWN, "DCA-ERR-005").check(arch);
  }

  @Test
  @DisplayName(
      "DCA-ERR-002 does not report the configured base type as an exception out of its layer")
  void theConfiguredBaseTypeIsNotTheProjectsOwnException() {
    DcaMarkers roles = OWN;

    assertTrue(
        roles.declaresTypesIn(VOCABULARY),
        "the package the configured base type lives in is the vocabulary's own: " + roles.roles());
    assertTrue(roles.declaringPackagePatterns().contains(VOCABULARY + ".."), roles.toString());
  }

  @Test
  @DisplayName(
      "a marker carried through a base class is selected, not only an implemented interface")
  void aMarkerReachedThroughABaseClassIsSelected() {
    DcaMarkers roles =
        DcaMarkers.dca()
            .named("own")
            .withDomainException(MISPLACED + ".vocabulary.ContractFailure");

    AssertionError failure =
        assertThrows(
            AssertionError.class,
            () -> rule(MISPLACED, roles, "DCA-ERR-002").check(arch(MISPLACED, roles)));

    assertTrue(failure.getMessage().contains("TariffRejectedException"), failure.getMessage());
    assertTrue(failure.getMessage().contains("domain layer"), failure.getMessage());
  }

  @Test
  @DisplayName("the report names the vocabulary and the roles that differ")
  void theReportNamesTheVocabulary() {
    assertEquals("dca (library default)", DcaLayout.forBasePackage(FIXTURE).markersReport());

    String report = DcaLayout.forBasePackage(FIXTURE).withMarkers(OWN).markersReport();

    assertTrue(report.startsWith("own ("), report);
    assertTrue(report.contains("aggregateRoot=" + VOCABULARY + ".ContractRoot"), report);
    assertFalse(report.contains("entity="), report);
  }

  @Test
  @DisplayName("an unknown role name fails instead of configuring nothing")
  void anUnknownRoleIsRefused() {
    IllegalArgumentException refused =
        assertThrows(
            IllegalArgumentException.class, () -> DcaMarkers.dca().withRole("agregateRoot", "x.Y"));

    assertTrue(refused.getMessage().contains("aggregateRoot"), refused.getMessage());
  }

  @Test
  @DisplayName("a role must name a type: an empty one would silently select nothing")
  void anEmptyRoleIsRefused() {
    IllegalArgumentException refused =
        assertThrows(IllegalArgumentException.class, () -> DcaMarkers.dca().withRepository(""));

    assertTrue(refused.getMessage().contains("repository"), refused.getMessage());
  }
}
