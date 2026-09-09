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
      assertFalse(preset.persistenceEntity().isEmpty(), preset.name());
      assertEquals(preset.name(), preset.toString());
    }
    FrameworkAnnotations none = FrameworkAnnotations.none();
    assertTrue(none.injectable().isEmpty());
    assertTrue(none.transactional().isEmpty());
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
