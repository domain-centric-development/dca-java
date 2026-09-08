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
}
