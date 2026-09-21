package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import java.util.List;
import org.junit.jupiter.api.Test;

class DcaLayoutTest {

  private static final DcaLayout DEFAULTS = DcaLayout.forBasePackage("com.acme.shop");

  @Test
  void overridesChangeOneSettingAndKeepTheRest() {
    DcaLayout layout =
        DEFAULTS
            .withIncomingSubpackage("in")
            .withApiSubpackage("contract")
            .withUseCaseSuffix("Service");

    assertEquals("com.acme.shop", layout.basePackage());
    assertEquals("in", layout.incomingSubpackage());
    assertEquals("outgoing", layout.outgoingSubpackage());
    assertEquals("contract", layout.apiSubpackage());
    assertEquals("events", layout.eventsSubpackage());
    assertEquals("Service", layout.useCaseSuffix());
    assertEquals("Resource", layout.restControllerSuffix());
    assertEquals("Controller", layout.controllerSuffix());
    assertEquals("Page", DEFAULTS.withControllerSuffix("Page").controllerSuffix());
    assertEquals(
        DEFAULTS.thirdPartyPackagesAllowedInDomain(), layout.thirdPartyPackagesAllowedInDomain());
    assertEquals("incoming", DEFAULTS.incomingSubpackage(), "the original is untouched");
  }

  @Test
  void allowingInDomainAppendsToTheDefaults() {
    DcaLayout layout = DEFAULTS.allowingInDomain("org.jmolecules..");

    assertTrue(
        layout
            .thirdPartyPackagesAllowedInDomain()
            .containsAll(DEFAULTS.thirdPartyPackagesAllowedInDomain()));
    assertTrue(layout.thirdPartyPackagesAllowedInDomain().contains("org.jmolecules.."));
  }

  @Test
  void aBasePackageMayHaveSeveralSegments() {
    assertEquals("com.acme.shop.infrastructure", DEFAULTS.infrastructurePackage());
    assertEquals(
        "com.acme.shop.cart.infrastructure", DEFAULTS.infrastructurePackage("com.acme.shop.cart"));
  }

  @Test
  void aSegmentMustBeExactlyOneIdentifier() {
    assertThrows(
        IllegalArgumentException.class, () -> DEFAULTS.withDomainSubpackage("domain.model"));
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withDomainSubpackage(" "));
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withDomainSubpackage("1domain"));
  }

  @Test
  void aBasePackageMustBeAPackageName() {
    assertThrows(IllegalArgumentException.class, () -> DcaLayout.forBasePackage("com..acme"));
    assertThrows(IllegalArgumentException.class, () -> DcaLayout.forBasePackage("com.acme.."));
    assertThrows(IllegalArgumentException.class, () -> DcaLayout.forBasePackage(""));
  }

  @Test
  void aSuffixIsPartOfAClassName() {
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withUseCaseSuffix("Use Case"));
    assertEquals(
        "ApplicationService", DEFAULTS.withUseCaseSuffix("ApplicationService").useCaseSuffix());
  }

  @Test
  void modelAndEventConsumerSegmentsAreLayoutSettings() {
    assertEquals("model", DEFAULTS.modelSubpackage());
    assertEquals("event", DEFAULTS.incomingEventSubpackage());
    DcaLayout layout =
        DEFAULTS.withModelSubpackage("entities").withIncomingEventSubpackage("listener");
    assertEquals("com.acme.cart.domain.entities..", layout.domainModelPattern("com.acme.cart"));
    assertEquals("..adapter.incoming.listener..", layout.incomingEventAdapterPattern());
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withModelSubpackage("a.b"));
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withIncomingEventSubpackage(""));
  }

  @Test
  void publishedSegmentsMustDiffer() {
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withEventsSubpackage("api"));
  }

  @Test
  void channelsMapOntoTheConfiguredPublishedSegments() {
    DcaLayout layout = DEFAULTS.withApiSubpackage("contract").withEventsSubpackage("published");

    assertEquals("contract", layout.channelSubpackage(Upstream.Consumes.API));
    assertEquals("published", layout.channelSubpackage(Upstream.Consumes.EVENTS));
    assertEquals(List.of("contract", "published"), layout.publishedSubpackages());
  }

  /**
   * The web adapter and shared segments are configurable like every other one: DCA-NAM-011 reads
   * the first, the shared-output-port patterns read the second, and the shared segment stays
   * reserved against the operation containers whatever it is called.
   */
  @Test
  void theWebAdapterAndSharedSegmentsAreConfigurable() {
    assertEquals("web", DEFAULTS.webSubpackage());
    assertEquals("shared", DEFAULTS.sharedSubpackage());
    assertEquals("ui", DEFAULTS.withWebSubpackage("ui").webSubpackage());

    DcaLayout ports = DEFAULTS.withSharedSubpackage("ports");
    assertEquals("com.acme.shop.*.application.ports..", ports.sharedOutputPortPattern());
    assertEquals(
        "com.acme.cart.application.ports..", ports.sharedOutputPortPattern("com.acme.cart"));

    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withOperationContainers("shared"));
    assertThrows(IllegalArgumentException.class, () -> ports.withOperationContainers("ports"));
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withWebSubpackage(""));
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withSharedSubpackage("a.b"));
  }

  /**
   * The tactical suffixes and the domain-service segment are configurable like the use-case and
   * controller suffixes; a project that calls its ports differently configures them rather than
   * excluding the rule ids.
   */
  @Test
  void theTacticalNamesAreConfigurable() {
    assertEquals("AggregateRoot", DEFAULTS.aggregateRootSuffix());
    assertEquals("Repository", DEFAULTS.repositorySuffix());
    assertEquals("Store", DEFAULTS.storeSuffix());
    assertEquals("Factory", DEFAULTS.factorySuffix());
    assertEquals("Specification", DEFAULTS.specificationSuffix());
    assertEquals("service", DEFAULTS.domainServiceSubpackage());

    DcaLayout own =
        DEFAULTS
            .withAggregateRootSuffix("Root")
            .withRepositorySuffix("Gateway")
            .withStoreSuffix("Table")
            .withFactorySuffix("Builder")
            .withSpecificationSuffix("Rule")
            .withDomainServiceSubpackage("policy");

    assertEquals("Root", own.aggregateRootSuffix());
    assertEquals("Gateway", own.repositorySuffix());
    assertEquals("Table", own.storeSuffix());
    assertEquals("Builder", own.factorySuffix());
    assertEquals("Rule", own.specificationSuffix());
    assertEquals("policy", own.domainServiceSubpackage());
    assertEquals("com.acme.shop", own.basePackage(), "the rest is unchanged");

    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withRepositorySuffix(""));
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withStoreSuffix("a.b"));
    assertThrows(IllegalArgumentException.class, () -> DEFAULTS.withDomainServiceSubpackage("a.b"));
  }
}
