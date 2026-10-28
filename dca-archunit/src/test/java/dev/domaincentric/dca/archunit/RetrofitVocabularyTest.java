package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * A project retrofitted with its own vocabulary: its own strategic and integration-event
 * annotations (retained in the class file only), its own input-port naming, a boot module named
 * like a layer, an adapter-internal store abstraction, sealed domain outcomes, enum results and a
 * local output port. Each convention is configured, none is switched off.
 */
class RetrofitVocabularyTest {

  private static final String APP = Fixtures.ROOT + ".retrofit.app";
  private static final String VOCABULARY = Fixtures.ROOT + ".retrofit.vocabulary.";

  private static DcaLayout configured() {
    return DcaLayout.forBasePackage(APP)
        .withExcludedPackages(APP + ".application")
        .withUseCaseSuffix("ApplicationService")
        .withInputPortSuffix("UseCase")
        .withLocalOutputPorts(VOCABULARY + "LocalPort")
        .withMarkers(
            DcaMarkers.dca()
                .named("retrofit")
                .withBoundedContext(VOCABULARY + "BoundedContext")
                .withSharedKernel(VOCABULARY + "SharedKernel")
                .withIntegrationEventType(VOCABULARY + "IntegrationEventType")
                .withIntegrationEvent(VOCABULARY + "IntegrationEvent")
                .withInputPort(VOCABULARY + "InputPort")
                .withUseCase(VOCABULARY + "InputPort")
                .withOutputPort(VOCABULARY + "OutputPort")
                .withValue(VOCABULARY + "Value"));
  }

  private static DcaArchitecture arch(DcaLayout layout) {
    return DcaArchitecture.of(layout, new ClassFileImporter().importPackages(APP));
  }

  private static DcaRule rule(DcaLayout layout, String id) {
    return DcaRules.all(layout).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no such rule: " + id));
  }

  private static void passes(DcaLayout layout, String id) {
    rule(layout, id).check(arch(layout));
  }

  private static AssertionError fails(DcaLayout layout, String id) {
    return assertThrows(AssertionError.class, () -> rule(layout, id).check(arch(layout)), id);
  }

  @Test
  void theOwnContextAnnotationIsReadFromTheClassFile() {
    DcaArchitecture arch = arch(configured());

    assertEquals(java.util.Set.of(APP + ".billing"), arch.boundedContexts().keySet());
    assertEquals("Billing", arch.boundedContexts().get(APP + ".billing").name());
    assertEquals(
        "Invoices and their payment", arch.boundedContexts().get(APP + ".billing").description());
  }

  @Test
  void theOwnSharedKernelAnnotationDeclaresTheKernel() {
    assertEquals(Optional.of(APP + ".common"), arch(configured()).sharedKernelPackage());
  }

  @Test
  void withoutTheRolesTheOwnAnnotationsAreNotSeen() {
    DcaArchitecture arch = arch(DcaLayout.forBasePackage(APP));

    assertTrue(arch.boundedContexts().isEmpty());
    assertTrue(arch.sharedKernelPackage().isEmpty());
  }

  @Test
  void aBootModuleNamedLikeALayerIsNamedAsTheCause() {
    DcaArchitecture arch = arch(DcaLayout.forBasePackage(APP));

    assertTrue(arch.moduleRoots().contains(APP));
    fails(DcaLayout.forBasePackage(APP).withMarkers(configured().markers()), "DCA-HEX-007");
    assertTrue(arch.baseModuleRootNotice().orElseThrow().contains(APP + ".application"));
  }

  @Test
  void theExcludedBootModuleLeavesTheContextsApart() {
    DcaArchitecture arch = arch(configured());

    assertFalse(arch.moduleRoots().contains(APP));
    assertTrue(arch.baseModuleRootNotice().isEmpty());
    assertFalse(arch.classes().contain(APP + ".application.Main"));
    passes(configured(), "DCA-HEX-007");
    passes(configured(), "DCA-HEX-002");
  }

  @Test
  void theBasePackageCannotBeExcluded() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DcaLayout.forBasePackage(APP).withExcludedPackages(APP));
  }

  @Test
  void theInputPortSuffixIsConfigurable() {
    assertTrue(
        fails(DcaLayout.forBasePackage(APP).withMarkers(configured().markers()), "DCA-NAM-003")
            .getMessage()
            .contains("PayInvoiceUseCase"));
    passes(configured(), "DCA-NAM-003");
    passes(configured(), "DCA-NAM-001");
  }

  @Test
  void theOwnInputPortIsNotARedeclaration() {
    passes(configured(), "DCA-USE-001");
  }

  @Test
  void theOwnIntegrationEventTypeCounts() {
    DcaLayout withoutRole =
        configured()
            .withMarkers(configured().markers().withRole("integrationEventType", "x.y.Missing"));

    assertTrue(fails(withoutRole, "DCA-ADV-005").getMessage().contains("InvoicePaid"));
    passes(configured(), "DCA-ADV-005");
  }

  @Test
  void anAdaptersOwnStoreAbstractionIsNotAPort() {
    passes(configured(), "DCA-TAC-018");
  }

  @Test
  void sealedDomainOutcomesAndEnumResultsPass() {
    passes(configured(), "DCA-USE-006");
    passes(configured(), "DCA-USE-007");
  }

  @Test
  void aLocalOutputPortIsNotRemote() {
    DcaLayout withoutLocal = configured().withLocalOutputPorts();

    assertTrue(fails(withoutLocal, "DCA-USE-013").getMessage().contains("SecretHasher"));
    passes(configured(), "DCA-USE-013");
  }

  @Test
  void theConventionalKernelPackageIsTheKernelWhenNothingIsDeclared() {
    String base = Fixtures.ROOT + ".retrofitkernel";

    assertEquals(Optional.of(base + ".sharedkernel"), Fixtures.arch(base).sharedKernelPackage());
  }

  @Test
  void aMissingMemberIsNamed() {
    String base = Fixtures.ROOT + ".retrofitnoname";
    DcaLayout layout =
        DcaLayout.forBasePackage(base)
            .withMarkers(DcaMarkers.dca().withBoundedContext(base + ".ContextLabel"));
    DcaArchitecture arch = DcaArchitecture.of(layout, new ClassFileImporter().importPackages(base));

    IllegalStateException error =
        assertThrows(
            IllegalStateException.class,
            () -> arch.boundedContexts().values().forEach(BoundedContextName::of));
    assertTrue(error.getMessage().contains("ContextLabel"), error.getMessage());
    assertTrue(error.getMessage().contains("name()"), error.getMessage());
  }

  /** Reads the one member every context declaration must carry. */
  private static final class BoundedContextName {
    static String of(dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext context) {
      return context.name();
    }
  }
}
