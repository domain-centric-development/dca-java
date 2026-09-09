package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.contextmap.ContextMapRenderer;
import dev.domaincentric.dca.archunit.rules.LayeredRules;
import org.junit.jupiter.api.Test;

class RetirementTest {
  @Test
  void publishedExclusionsAndSeverityPropertiesStillLoad() {
    for (String id : DcaRules.retired().keySet()) {
      assertFalse(DcaRules.allIds().contains(id));
      assertDoesNotThrow(() -> DcaRuleSelection.all().excluding(id));
      var properties = new java.util.Properties();
      properties.setProperty("dca.rules.off", id);
      properties.setProperty("dca.rules.warn", id);
      assertDoesNotThrow(() -> DcaRuleSelection.fromProperties(properties));
    }
    assertThrows(
        IllegalArgumentException.class, () -> DcaRuleSelection.all().excluding("DCA-ADV-999"));
  }

  @Test
  void informationalEntriesHaveExplicitKind() {
    var layout = DcaLayout.forBasePackage("example");
    assertEquals(
        java.util.Set.of("DCA-LAY-001", "DCA-STR-001", "DCA-STR-010", "DCA-MAP-013", "DCA-NAM-002"),
        DcaRules.all(layout).stream()
            .filter(r -> r.kind() == DcaRule.Kind.INFORMATIONAL)
            .map(DcaRule::id)
            .collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void consumerImplementationInReservedOutputPackageFailsButImportedMarkerPasses() {
    var layout = DcaLayout.forBasePackage("example");
    var rule = new LayeredRules(layout).outputPortMarkersMustBeInterfaces();
    var importer = new ClassFileImporter();
    rule.check(
        DcaArchitecture.of(
            layout,
            importer.importClasses(
                dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort.class)));
    var bad =
        DcaArchitecture.of(
            layout,
            importer.importClasses(
                dev.domaincentric.dca.buildingblocks.hexagonal.port.out.ConsumerImplementation
                    .class));
    assertTrue(
        assertThrows(AssertionError.class, () -> rule.check(bad))
            .getMessage()
            .contains("ConsumerImplementation"));
  }

  @Test
  void rendererDisambiguatesExternalNamesWithoutRenamingThem() {
    var arch = Fixtures.arch(Fixtures.ROOT + ".contextmap.bad");
    String md = ContextMapRenderer.of(arch).render();
    assertTrue(md.contains("_2[["), md);
  }
}
