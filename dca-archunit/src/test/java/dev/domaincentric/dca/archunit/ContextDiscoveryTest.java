package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Context discovery is by {@code @BoundedContext}, not by position in the package tree, so a
 * context may sit at any depth. These fixtures cover the three layouts that the former {@code
 * base.*.domain..} wildcard made impossible, and pin the failure mode that made the loss invisible:
 * a context nested one level too deep used to pass every layer rule for lack of subjects.
 */
class ContextDiscoveryTest {

  private static final String FIXTURES = "dev.domaincentric.dca.archunit.fixtures.layout";

  private static DcaArchitecture arch(String basePackage) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(basePackage), new ClassFileImporter().importPackages(basePackage));
  }

  private static DcaRule rule(String id, String basePackage) {
    return DcaRules.all(DcaLayout.forBasePackage(basePackage)).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new AssertionError("no such rule: " + id));
  }

  @Nested
  @DisplayName("a context grouped below an intermediate package")
  class Grouped {

    private static final String BASE = FIXTURES + ".grouped";

    @Test
    void isDiscoveredAtDepthTwo() {
      assertEquals(List.of(BASE + ".sales.order"), arch(BASE).boundedContextPackages());
    }

    @Test
    void isNamedByItsTrailingName() {
      DcaArchitecture arch = arch(BASE);
      assertEquals("sales.order", arch.contextName(BASE + ".sales.order"));
    }

    @Test
    void resolvesClassesToTheAnnotatedAncestorAndNotToTheGroup() {
      assertEquals(
          BASE + ".sales.order", arch(BASE).rootContextPackage(BASE + ".sales.order.domain.model"));
    }

    @Test
    void isGovernedByTheLayerRules() {
      assertThrows(AssertionError.class, () -> rule("DCA-LAY-002", BASE).check(arch(BASE)));
    }

    /** The web-adapter package of a grouped context is derived from its module root. */
    @Test
    void itsViewModelsAreInTheRightPlace() {
      rule("DCA-NAM-011", BASE).check(arch(BASE));
    }
  }

  @Nested
  @DisplayName("a single-context application whose base package is the context")
  class Flat {

    private static final String BASE = FIXTURES + ".flat";

    @Test
    void isDiscoveredAsItsOwnContext() {
      assertEquals(List.of(BASE), arch(BASE).boundedContextPackages());
    }

    @Test
    void isGovernedByTheLayerRules() {
      assertThrows(AssertionError.class, () -> rule("DCA-LAY-002", BASE).check(arch(BASE)));
    }

    /** {@code base.adapter.incoming.web} is a web-adapter package when the base is the context. */
    @Test
    void itsViewModelsAreInTheRightPlace() {
      rule("DCA-NAM-011", BASE).check(arch(BASE));
    }
  }

  @Nested
  @DisplayName("a context that declares no @BoundedContext")
  class Undeclared {

    private static final String BASE = FIXTURES + ".nested";

    @Test
    void isNotDiscovered() {
      assertTrue(arch(BASE).boundedContexts().isEmpty());
    }

    /**
     * The layer rules select structurally, over {@code moduleRoots()}, so a context nested one
     * level too deep is governed even before anyone declares it: {@code Todo} depends on
     * infrastructure and DCA-LAY-002 says so. This is the behaviour the {@code base.*.domain..}
     * wildcard could not deliver — under it the rule found no classes at all.
     */
    @Test
    void isStillGovernedByTheLayerRules() {
      AssertionError error =
          assertThrows(AssertionError.class, () -> rule("DCA-LAY-002", BASE).check(arch(BASE)));
      assertTrue(
          error.getMessage().contains("Todo"),
          "the violation itself must be reported, was: " + error.getMessage());
      assertFalse(
          error.getMessage().contains("failed to check any classes"),
          "must not be an empty-subject failure, was: " + error.getMessage());
    }

    @Test
    void isFoundAsAModuleRootDespiteItsDepth() {
      assertEquals(List.of(BASE + ".contexts.todo"), arch(BASE).moduleRoots());
    }

    /**
     * Isolation is structural too: the module is a subject of the isolation rules without any
     * declaration. What a missing declaration costs is only its place on the context map.
     */
    @Test
    void isASubjectOfTheIsolationRules() {
      assertEquals(List.of(BASE + ".contexts.todo"), arch(BASE).isolatedModuleRoots());
    }
  }

  @Nested
  @DisplayName("a non-context module grouped below an intermediate package")
  class GroupedModule {

    private static final String BASE = FIXTURES + ".groupedmodule";

    @Test
    void isNoBoundedContext() {
      assertTrue(arch(BASE).boundedContexts().isEmpty());
    }

    @Test
    void isFoundAsAModuleRootAtDepthTwo() {
      assertEquals(List.of(BASE + ".generic.backoffice"), arch(BASE).moduleRoots());
    }

    /** Its layers are governed although it declares no bounded context. */
    @Test
    void contributesItsLayerPatterns() {
      assertArrayEquals(
          new String[] {BASE + ".generic.backoffice.application.."},
          arch(BASE).allApplicationPatterns());
    }

    /**
     * DCA requires no declaration from it: it is a subject of the isolation rules as it stands, and
     * none of them — nor anything else in the catalog — asks it to declare what it is.
     */
    @Test
    void isIsolatedWithoutDeclaringAnything() {
      DcaArchitecture arch = arch(BASE);
      assertEquals(List.of(BASE + ".generic.backoffice"), arch.isolatedModuleRoots());
      for (String id : List.of("DCA-STR-003", "DCA-STR-004", "DCA-STR-006", "DCA-HEX-007")) {
        rule(id, BASE).check(arch);
      }
    }
  }

  @Nested
  @DisplayName("a bounded context declared before it has any code")
  class EmptyContext {

    private static final String BASE = FIXTURES + ".emptycontext";

    /**
     * A context is a boundary of language and ownership, not a file count — declaring it before the
     * model exists is legitimate, and discovery must see it so that it appears on the context map
     * and in the strategic rules from day one.
     */
    @Test
    void isDiscovered() {
      assertEquals(
          List.of(BASE + ".planned"), arch(BASE).boundedContexts().keySet().stream().toList());
    }

    /** It owns no layer yet, so it is no module root and the layer rules have nothing to say. */
    @Test
    void ownsNoModuleYet() {
      assertEquals(List.of(), arch(BASE).moduleRoots());
    }
  }

  @Nested
  @DisplayName("a bounded context in transaction-script style")
  class TransactionScript {

    private static final String BASE = FIXTURES + ".transactionscript";

    /**
     * A bounded context is a boundary of language; which tactical patterns live inside it is a
     * separate decision, taken per subdomain. A generic or supporting subdomain may legitimately be
     * a transaction script — a use case over a Store, no aggregate, no {@code domain/} package at
     * all — and nothing in DCA requires otherwise.
     */
    @Test
    void isADeclaredContextWithoutAnyDomainPackage() {
      DcaArchitecture arch = arch(BASE);
      assertEquals(List.of(BASE + ".reporting"), arch.boundedContextPackages());
      assertEquals(List.of(BASE + ".reporting"), arch.moduleRoots());
      assertTrue(
          arch.classes().stream()
              .map(c -> c.getPackageName().substring(BASE.length()))
              .noneMatch(p -> p.endsWith(".domain") || p.contains(".domain.")),
          "the fixture deliberately has no domain layer");
    }

    /** The full catalog against a context with no domain model. */
    @Test
    void passesTheWholeCatalog() {
      DcaArchitecture arch = arch(BASE);
      for (DcaRule rule : DcaRules.all(DcaLayout.forBasePackage(BASE))) {
        rule.check(arch);
      }
    }

    /**
     * A transaction-script context runs the **whole** catalog green, with no configuration.
     *
     * <p>It did not always: the six domain-layer rules below used to fail with ArchUnit's "failed
     * to check any classes" — an empty subject, not a violation, and a message naming the rule
     * instead of saying "this context has no domain, which is allowed". They now carry {@code
     * allowEmptyShould(true)}, on the same reasoning the use-case rules already did: pattern
     * selection is per subdomain, so an absent domain layer is a decision, not a defect.
     *
     * <p>This does not weaken the loud-failure property that matters. A module whose layers exist
     * is found structurally by {@link DcaArchitecture#moduleRoots()}, so its rules — layer and
     * isolation alike — have subjects. What is silenced is only the genuinely empty case.
     */
    @Test
    void passesTheDomainRulesItHasNoSubjectsFor() {
      DcaArchitecture arch = arch(BASE);
      for (String id : EMPTY_DOMAIN_SUBJECT) {
        rule(id, BASE).check(arch);
      }
    }
  }

  @Nested
  @DisplayName("a cycle between two grouped contexts")
  class GroupedCycle {

    private static final String BASE = FIXTURES + ".groupedcycle";

    @Test
    void bothContextsAreDiscoveredAtDepthTwo() {
      assertEquals(
          List.of(BASE + ".group.alpha", BASE + ".group.beta"),
          arch(BASE).boundedContextPackages().stream().sorted().toList());
    }

    /**
     * The acceptance test for the slice change. The cycle rules used to slice with {@code
     * slices().matching(base + ".(*)." + layer + "..")}, and {@code (*)} is one segment — so two
     * contexts grouped below an intermediate package produced no slices at all and their mutual
     * dependency went unreported. They now assign slices from the module root, so the cycle is
     * found.
     */
    @Test
    void isReported() {
      AssertionError error =
          assertThrows(AssertionError.class, () -> rule("DCA-CYC-002", BASE).check(arch(BASE)));
      assertTrue(
          error.getMessage().contains("Cycle") || error.getMessage().contains("cycle"),
          "expected a cycle violation, was: " + error.getMessage());
      assertTrue(
          error.getMessage().contains("alpha") && error.getMessage().contains("beta"),
          "should name both contexts, was: " + error.getMessage());
    }
  }

  /** The domain-layer rules that have no subject in a context without a domain layer. */
  private static final List<String> EMPTY_DOMAIN_SUBJECT =
      List.of(
          "DCA-LAY-002", "DCA-ONI-001", "DCA-ONI-002", "DCA-ONI-003", "DCA-HEX-001", "DCA-CYC-001");

  @Nested
  @DisplayName("rootContextPackage")
  class RootContextPackage {

    private static final String BASE = FIXTURES + ".grouped";

    @Test
    void returnsNullOutsideTheBasePackage() {
      assertNull(arch(BASE).rootContextPackage("com.example.elsewhere"));
    }

    @Test
    void returnsNullForNull() {
      assertNull(arch(BASE).rootContextPackage(null));
    }

    @Test
    void resolvesADeeplyNestedClassToItsContext() {
      assertEquals(
          BASE + ".sales.order",
          arch(BASE).rootContextPackage(BASE + ".sales.order.adapter.outgoing.persistence.jdbc"));
    }

    /**
     * A package named {@code domain} inside an adapter — an outgoing adapter mapping to a foreign
     * model — stays part of its context and must not be mistaken for a layer of its own.
     */
    @Test
    void keepsADomainNamedAdapterPackageInsideItsContext() {
      assertEquals(
          BASE + ".sales.order",
          arch(BASE).rootContextPackage(BASE + ".sales.order.adapter.outgoing.domain"));
    }

    @Test
    void fallsBackToTheFirstSegmentWhenNothingIsDeclared() {
      String base = FIXTURES + ".nested";
      assertEquals(
          base + ".contexts", arch(base).rootContextPackage(base + ".contexts.todo.domain.model"));
    }
  }
}
