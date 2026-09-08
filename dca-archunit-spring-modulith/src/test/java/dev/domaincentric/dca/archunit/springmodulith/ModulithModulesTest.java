package dev.domaincentric.dca.archunit.springmodulith;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.DcaLayout;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.core.Violations;

class ModulithModulesTest {

  private static final String FIXTURE = "dev.domaincentric.dca.archunit.springmodulith.fixture";
  private static final DcaLayout LAYOUT = DcaLayout.forBasePackage(FIXTURE);

  @Test
  void excludesTestClassesWithTheirNestedAndClosureClasses() {
    for (String name :
        Set.of(
            "com.acme.FooTest",
            "com.acme.FooTests",
            "com.acme.FooSpec",
            "com.acme.FooIT",
            "com.acme.FooTest$1",
            "com.acme.FooTest$Helper",
            "com.acme.FooSpec$_check_closure1",
            "com.acme.FooSpec$_run_closure2$_closure3")) {
      assertTrue(ModulithModules.isTestClass(name), name);
    }
    for (String name :
        Set.of("com.acme.Foo", "com.acme.TestData", "com.acme.Contest", "com.acme.Order$Line")) {
      assertFalse(ModulithModules.isTestClass(name), name);
    }
  }

  @Test
  void withoutTheFilterTheBasePackageTestClassBreaksVerification() {
    ApplicationModules raw = ApplicationModules.of(FIXTURE);
    assertThrows(
        Violations.class, raw::verify, "the *Test class in the base package is a root module");
  }

  @Test
  void withTheFilterTheFixtureVerifies() {
    ApplicationModules modules = ModulithModules.of(LAYOUT);
    assertDoesNotThrow(() -> modules.verify());
    assertEquals(
        Set.of("orders", "shipping"),
        modules.stream()
            .map(m -> m.getIdentifier().toString())
            .collect(java.util.stream.Collectors.toSet()));
  }

  @Test
  void rejectsNull() {
    assertThrows(IllegalArgumentException.class, () -> ModulithModules.of(null));
    assertFalse(ModulithModules.isTestClass(null));
  }
}
