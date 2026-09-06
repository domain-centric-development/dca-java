package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.archunit.DcaRules;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Features — optional, domain-named groups of use cases below a module's application package
 * ({@code application.<feature>.<usecase>}) — and the two rules that keep them legible: {@code
 * DCA-USE-014} (one consistent use-case depth per module) and {@code DCA-CYC-005} (no cycles
 * between the feature or use-case slices of one module).
 *
 * <p>The compatibility fixture pins that a grouped context is governed by the pre-existing catalog
 * exactly like a flat one: every rule that selects with {@code ..} sees the nested packages, and a
 * later change of a selector into a direct-child assumption fails here first.
 */
class FeatureLayoutTest {

  private static final String FIXTURES = "dev.domaincentric.dca.archunit.fixtures.features";

  static DcaArchitecture arch(String pkg) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg), new ClassFileImporter().importPackages(pkg));
  }

  static DcaRule rule(String pkg, String id) {
    return DcaRules.all(DcaLayout.forBasePackage(pkg)).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  static List<String> failures(String pkg) {
    DcaArchitecture arch = arch(pkg);
    List<String> messages = new ArrayList<>();
    for (DcaRule rule : DcaRules.all(arch.layout())) {
      try {
        rule.check(arch);
      } catch (AssertionError e) {
        messages.add(rule.id() + " :: " + e.getMessage().replace('\n', ' '));
      }
    }
    return messages;
  }

  @Nested
  @DisplayName("a feature-grouped context under the whole catalog")
  class Compatibility {

    private static final String COMPAT = FIXTURES + ".compat";

    @Test
    @DisplayName("the grouped context is discovered as one module with one bounded context")
    void oneModuleOneContext() {
      DcaArchitecture arch = arch(COMPAT);
      assertEquals(List.of(COMPAT + ".sales"), arch.isolatedModuleRoots());
      assertTrue(arch.boundedContexts().containsKey(COMPAT + ".sales"));
    }

    @Test
    @DisplayName("every rule of the catalog passes — nested feature packages stay governed")
    void wholeCatalogPasses() {
      assertEquals(List.of(), failures(COMPAT));
    }

    @Test
    @DisplayName("the feature package is seen by the application-layer selectors")
    void featurePackagesAreSelected() {
      DcaArchitecture arch = arch(COMPAT);
      String[] patterns = arch.allApplicationPatterns();
      assertEquals(1, patterns.length);
      assertTrue(patterns[0].endsWith(".application.."), patterns[0]);
      assertTrue(
          arch.classes().stream()
              .anyMatch(
                  c ->
                      c.getPackageName().endsWith(".application.ordering.placeorder")
                          && c.getSimpleName().equals("PlaceOrderUseCase")),
          "fixture must contain the grouped use case");
    }
  }

  @Nested
  @DisplayName("DCA-USE-014 — one consistent use-case depth per module")
  class UseCaseDepth {

    private static final String DEPTH = FIXTURES + ".depth";
    private static final String ID = "DCA-USE-014";

    private void passes(String shape) {
      String pkg = DEPTH + "." + shape;
      assertDoesNotThrow(() -> rule(pkg, ID).check(arch(pkg)), shape);
    }

    private DcaRuleViolation fails(String shape) {
      String pkg = DEPTH + "." + shape;
      return assertThrows(DcaRuleViolation.class, () -> rule(pkg, ID).check(arch(pkg)), shape);
    }

    @Test
    void flatLayoutPasses() {
      passes("flat");
    }

    @Test
    void groupedLayoutPasses() {
      passes("grouped");
    }

    @Test
    @DisplayName("an abstract base class named like a use case does not count")
    void abstractBaseClassesAreIgnored() {
      // both fixtures carry application.support.BaseUseCase — depth 2 in the flat layout, and an
      // extra package in the grouped one; neither may turn into a mixed-depth violation
      passes("flat");
      passes("grouped");
    }

    @Test
    @DisplayName("a single use case may use either depth")
    void singleUseCasePasses() {
      passes("single");
      passes("singleflat");
    }

    @Test
    @DisplayName("a module without use cases is valid")
    void noUseCasesPasses() {
      passes("nousecase");
    }

    @Test
    @DisplayName("a use case directly in the application package is reported")
    void shallowFails() {
      DcaRuleViolation v = fails("shallow");
      assertEquals(1, v.violations().size(), v.getMessage());
      assertTrue(
          v.violations().get(0).contains("directly in the application package"), v.getMessage());
    }

    @Test
    @DisplayName("a use case nested deeper than application.<feature>.<usecase> is reported")
    void overDeepFails() {
      DcaRuleViolation v = fails("overdeep");
      assertEquals(1, v.violations().size(), v.getMessage());
      assertTrue(v.violations().get(0).contains("nested deeper"), v.getMessage());
      assertTrue(
          v.violations().get(0).contains("application.ordering.placement.placeorder"),
          v.getMessage());
    }

    @Test
    @DisplayName("mixing flat and grouped use cases in one module is reported once, naming both")
    void mixedFails() {
      DcaRuleViolation v = fails("mixed");
      assertEquals(1, v.violations().size(), v.getMessage());
      String message = v.violations().get(0);
      assertTrue(message.contains("mixes flat use case packages"), message);
      assertTrue(message.contains("application.placeorder"), message);
      assertTrue(message.contains("application.fulfilment.shiporder"), message);
      assertTrue(message.contains(DEPTH + ".mixed.ordering"), message);
    }
  }

  @Nested
  @DisplayName("DCA-CYC-005 — feature and use-case slices of one module are free of cycles")
  class ApplicationSlices {

    private static final String SLICES = FIXTURES + ".slices";
    private static final String ID = "DCA-CYC-005";

    private void passes(String shape) {
      String pkg = SLICES + "." + shape;
      assertDoesNotThrow(() -> rule(pkg, ID).check(arch(pkg)), shape);
    }

    private AssertionError fails(String shape) {
      String pkg = SLICES + "." + shape;
      return assertThrows(AssertionError.class, () -> rule(pkg, ID).check(arch(pkg)), shape);
    }

    @Test
    void independentFeaturesPass() {
      passes("independent");
    }

    @Test
    void oneDirectionalDependencyPasses() {
      passes("oneway");
    }

    @Test
    @DisplayName("a dependency on application.shared is not a slice dependency")
    void sharedIsNotASlice() {
      passes("shared");
    }

    @Test
    @DisplayName("a cycle between two features names both slices")
    void featureCycleFails() {
      String message = fails("cycle").getMessage();
      assertTrue(message.contains("application.ordering"), message);
      assertTrue(message.contains("application.fulfilment"), message);
    }

    @Test
    @DisplayName("flat use-case packages are sliced the same way")
    void flatUseCasesBehaveAlike() {
      passes("flatoneway");
      String message = fails("flatcycle").getMessage();
      assertTrue(message.contains("application.placeorder"), message);
      assertTrue(message.contains("application.shiporder"), message);
    }
  }
}
