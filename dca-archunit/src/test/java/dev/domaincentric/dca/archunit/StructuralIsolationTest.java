package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Isolation is structural. The isolation rules ({@code DCA-STR-003}, {@code DCA-STR-004}, {@code
 * DCA-STR-006}, {@code DCA-HEX-007}) select over {@link DcaArchitecture#isolatedModuleRoots()} —
 * every module that owns a DCA layer — and not over the declared bounded contexts. A module that
 * declares no {@code @BoundedContext} is therefore governed as a source and protected as a target
 * exactly like a declared one; the declaration decides context-map membership and nothing else.
 *
 * <p>The fixture puts the identical violation in two places — {@code peer}, a declared context, and
 * {@code reporting}, an undeclared module — so the difference is the declaration and nothing else.
 * It used to pin the opposite: before isolation went structural, only {@code peer} was reported.
 */
class StructuralIsolationTest {

  private static final String BASE = "dev.domaincentric.dca.archunit.fixtures.layout.isolation";
  private static final String INTERNALS = BASE + ".catalog.domain.model";

  private static DcaArchitecture arch() {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(BASE), new ClassFileImporter().importPackages(BASE));
  }

  /** Every failure of the full catalog, as one message per rule id. */
  private static List<String> failures(DcaArchitecture arch) {
    List<String> messages = new ArrayList<>();
    for (DcaRule rule : DcaRules.all(DcaLayout.forBasePackage(BASE))) {
      try {
        rule.check(arch);
      } catch (AssertionError e) {
        messages.add(rule.id() + " :: " + e.getMessage().replace('\n', ' '));
      }
    }
    return messages;
  }

  private static List<String> failuresOf(String ruleId) {
    return failures(arch()).stream().filter(m -> m.startsWith(ruleId + " ")).toList();
  }

  @Test
  @DisplayName("the fixture differs in the declaration and in nothing else")
  void bothModulesOwnLayersAndOneIsDeclared() {
    DcaArchitecture arch = arch();
    assertTrue(
        arch.isolatedModuleRoots()
            .containsAll(List.of(BASE + ".catalog", BASE + ".peer", BASE + ".reporting")),
        "all three must be isolated module roots: " + arch.isolatedModuleRoots());
    assertTrue(arch.boundedContexts().containsKey(BASE + ".peer"), "peer is declared");
    assertFalse(arch.boundedContexts().containsKey(BASE + ".reporting"), "reporting is not");
  }

  @Test
  @DisplayName("a declared context is caught importing another module's internals")
  void theDeclaredSourceIsReported() {
    assertTrue(
        failuresOf("DCA-STR-003").stream()
            .anyMatch(m -> m.contains("GetPeerUseCase") && m.contains(INTERNALS)),
        "DCA-STR-003 must report peer: " + failuresOf("DCA-STR-003"));
  }

  @Test
  @DisplayName("an undeclared module is caught doing the same thing")
  void theUndeclaredSourceIsReportedToo() {
    assertTrue(
        failuresOf("DCA-STR-003").stream()
            .anyMatch(m -> m.contains("GetReportUseCase") && m.contains(INTERNALS)),
        "DCA-STR-003 must report reporting although it declares nothing: "
            + failuresOf("DCA-STR-003"));
  }

  @Test
  @DisplayName("an undeclared module is protected as a target")
  void theUndeclaredTargetIsProtected() {
    assertTrue(
        failuresOf("DCA-STR-003").stream()
            .anyMatch(
                m ->
                    m.contains("DescribeProductUseCase")
                        && m.contains(BASE + ".reporting.application")),
        "DCA-STR-003 must report catalog reaching into reporting: " + failuresOf("DCA-STR-003"));
  }

  @Test
  @DisplayName("an undeclared module's incoming adapter must stay in its own module")
  void theUndeclaredIncomingAdapterIsReported() {
    assertTrue(
        failuresOf("DCA-HEX-007").stream().anyMatch(m -> m.contains("ReportController")),
        "DCA-HEX-007 must report reporting's controller: " + failuresOf("DCA-HEX-007"));
  }

  @Test
  @DisplayName("the domain layer may not depend on another module, not even its api")
  void theDomainLayerIsIsolatedFromForeignApi() {
    assertTrue(
        failuresOf("DCA-STR-004").stream().anyMatch(m -> m.contains("PeerListing")),
        "DCA-STR-004 must report peer's domain: " + failuresOf("DCA-STR-004"));
  }

  @Test
  @DisplayName("an outgoing adapter may use a foreign api package but not foreign internals")
  void theOutgoingAdapterAllowListIsApiAndEvents() {
    List<String> str006 = failuresOf("DCA-STR-006");
    assertTrue(
        str006.stream().anyMatch(m -> m.contains("CatalogInternalsClient")),
        "DCA-STR-006 must report the adapter using the catalog's domain model: " + str006);
    assertFalse(
        str006.stream().anyMatch(m -> m.contains("CatalogClient") && !m.contains("Internals")),
        "DCA-STR-006 must not report the adapter using the catalog's api: " + str006);
  }

  /**
   * The allow-list is a layout setting, like every other package segment. Renaming the published
   * package makes the former {@code api} package internal — the same adapter that passed is
   * reported.
   */
  @Test
  @DisplayName("the published packages are configured on the layout")
  void thePublishedPackagesComeFromTheLayout() {
    DcaLayout layout = DcaLayout.forBasePackage(BASE).withApiSubpackage("contract");
    DcaArchitecture arch = DcaArchitecture.of(layout, new ClassFileImporter().importPackages(BASE));
    assertTrue(
        List.of(arch.publishedPackagePatternsExcluding(BASE + ".peer"))
            .contains(BASE + ".catalog.contract.."),
        "the allow-list must follow the layout: "
            + List.of(arch.publishedPackagePatternsExcluding(BASE + ".peer")));
    DcaRule str006 =
        DcaRules.all(layout).stream()
            .filter(r -> r.id().equals("DCA-STR-006"))
            .findFirst()
            .orElseThrow();
    AssertionError error = assertThrows(AssertionError.class, () -> str006.check(arch));
    assertTrue(
        error.getMessage().contains("CatalogClient"),
        "with api renamed, the catalog's api package is internal and CatalogClient is reported: "
            + error.getMessage());
  }

  @Test
  @DisplayName("the layout rejects identical api and events segments")
  void theLayoutRejectsIdenticalPublishedSegments() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DcaLayout.forBasePackage(BASE).withApiSubpackage("events"));
  }

  @Test
  @DisplayName("no rule asks a module to declare itself")
  void noDeclarationIsDemanded() {
    assertTrue(
        DcaRules.all(DcaLayout.forBasePackage(BASE)).stream()
            .noneMatch(r -> r.id().equals("DCA-LAY-006")),
        "DCA-LAY-006 was deleted with structural isolation; the id is free");
    assertTrue(
        failures(arch()).stream().noneMatch(m -> m.contains("declares nothing")),
        "nothing may demand a declaration");
  }
}
