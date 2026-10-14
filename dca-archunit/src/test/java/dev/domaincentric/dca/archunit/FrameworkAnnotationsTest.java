package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class FrameworkAnnotationsTest {

  @Test
  void everyPresetHasANameAndNoneIsEmpty() {
    for (FrameworkAnnotations preset :
        List.of(
            FrameworkAnnotations.spring(),
            FrameworkAnnotations.jakarta(),
            FrameworkAnnotations.quarkus(),
            FrameworkAnnotations.micronaut())) {
      assertFalse(preset.injectable().isEmpty(), preset.name());
      assertFalse(preset.transactional().isEmpty(), preset.name());
      assertFalse(preset.transactionApi().isEmpty(), preset.name());
      assertFalse(preset.transactionManager().isEmpty(), preset.name());
      assertFalse(preset.persistenceEntity().isEmpty(), preset.name());
      assertEquals(preset.name(), preset.toString());
    }
    FrameworkAnnotations none = FrameworkAnnotations.none();
    assertTrue(none.injectable().isEmpty());
    assertTrue(none.transactional().isEmpty());
    assertTrue(none.transactionApi().isEmpty());
    assertTrue(none.transactionManager().isEmpty());
    assertTrue(none.persistenceEntity().isEmpty());
    assertFalse(none.hasModuleDeclaration());
  }

  @Test
  void withReplacesOneRoleAndKeepsTheOthers() {
    FrameworkAnnotations adjusted =
        FrameworkAnnotations.jakarta().withRestController("com.acme.platform.Endpoint");

    assertEquals(List.of("com.acme.platform.Endpoint"), adjusted.restController());
    assertEquals(FrameworkAnnotations.jakarta().injectable(), adjusted.injectable());
    assertEquals("jakarta", adjusted.name());
    assertEquals("acme", adjusted.named("acme").name());
    assertEquals(List.of(), adjusted.withRestController().restController(), "no argument empties");
  }

  /** Every with* method and named() carry every other role, the later ones included. */
  @Test
  void everyAdjustmentKeepsEveryOtherRole() {
    FrameworkAnnotations spring = FrameworkAnnotations.spring();
    List<FrameworkAnnotations> adjusted =
        List.of(
            spring.named("adjusted"),
            spring.withInjectable("x.A"),
            spring.withWebController("x.A"),
            spring.withRestController("x.A"),
            spring.withTransactional("x.A"),
            spring.withEventListener("x.A"),
            spring.withModuleDeclaration("x.A"),
            spring.withPublishedInterface("x.A"),
            spring.withPersistenceEntity("x.A"),
            spring.withInjectionSite("x.A"),
            spring.withPersistenceMapping("x.A"),
            spring.withTransactionApi("x.A"),
            spring.withTransactionManager("x.A"));
    for (FrameworkAnnotations each : adjusted) {
      assertEquals(spring.transportStatus(), each.transportStatus(), each.toString());
      assertEquals(spring.getMapping(), each.getMapping(), each.toString());
      assertEquals(spring.instanceLocalState(), each.instanceLocalState(), each.toString());
    }
    assertEquals(List.of("x.Get"), spring.withGetMapping("x.Get").getMapping());
    assertEquals(
        List.of("x.Flash.keep"),
        spring.withInstanceLocalState("x.Flash.keep").instanceLocalState());
    assertEquals(spring.getMapping(), spring.withInstanceLocalState().getMapping());
  }

  @Test
  void rolesRejectBlankNames() {
    assertThrows(
        IllegalArgumentException.class, () -> FrameworkAnnotations.none().withInjectable(" "));
    assertThrows(IllegalArgumentException.class, () -> FrameworkAnnotations.none().named(""));
  }

  @Test
  void describeCollapsesToDistinctSimpleNames() {
    assertEquals(
        "@Transactional",
        FrameworkAnnotations.describe(
            List.of(
                "org.springframework.transaction.annotation.Transactional",
                "jakarta.transaction.Transactional"),
            "none"));
    assertEquals(
        "@Service/@Component",
        FrameworkAnnotations.describe(FrameworkAnnotations.spring().injectable(), "none"));
    assertEquals("none", FrameworkAnnotations.describe(List.of(), "none"));
  }

  @Test
  @SuppressWarnings("deprecation")
  void theOldShapeStillMapsOntoTheRoles() {
    FrameworkAnnotations custom =
        FrameworkAnnotations.of(
            "a.Service", "a.Component", "a.Controller", "a.Rest", "a.Tx", "a.Listener", null);

    assertEquals(List.of("a.Service", "a.Component"), custom.injectable());
    assertEquals("a.Service", custom.service());
    assertEquals("a.Component", custom.component());
    assertEquals("a.Controller", custom.controller());
    assertEquals(List.of("a.Rest"), custom.restController());
    assertEquals(List.of("a.Tx"), custom.transactional());
    assertNull(custom.applicationModule());
    assertFalse(custom.hasApplicationModule());
    assertEquals("custom", custom.name());

    FrameworkAnnotations spring = FrameworkAnnotations.spring();
    assertEquals("org.springframework.stereotype.Service", spring.service());
    assertEquals("org.springframework.stereotype.Component", spring.component());
    assertEquals("org.springframework.modulith.ApplicationModule", spring.applicationModule());
    assertNull(FrameworkAnnotations.none().service());
  }
}
