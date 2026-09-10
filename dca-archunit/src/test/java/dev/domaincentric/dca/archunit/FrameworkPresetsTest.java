package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * The catalog resolves its framework vocabulary through {@link FrameworkAnnotations}. Each preset
 * runs the whole catalog green against a fixture written in its idiom — and only the matching
 * preset sees the violations written in that idiom, which is what proves the rules read the preset
 * rather than Spring's names.
 */
class FrameworkPresetsTest {

  private static final String FIXTURES = Fixtures.ROOT + ".frameworks";

  private static DcaArchitecture arch(String preset, FrameworkAnnotations annotations) {
    String pkg = FIXTURES + "." + preset;
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(pkg).withFrameworkAnnotations(annotations),
        new ClassFileImporter().importPackages(pkg));
  }

  private static DcaRule rule(DcaArchitecture arch, String id) {
    return DcaRules.all(arch.layout()).stream()
        .filter(r -> r.id().equals(id))
        .findFirst()
        .orElseThrow();
  }

  @TestFactory
  @DisplayName("the full catalog runs green on a fixture written in the preset's idiom")
  Stream<DynamicTest> fullCatalogPassesPerPreset() {
    return Stream.of(
            new Object[] {"jakarta", FrameworkAnnotations.jakarta()},
            new Object[] {"jakarta", FrameworkAnnotations.quarkus()},
            new Object[] {"micronaut", FrameworkAnnotations.micronaut()},
            new Object[] {"none", FrameworkAnnotations.none()})
        .map(
            pair ->
                DynamicTest.dynamicTest(
                    pair[1] + " preset on the " + pair[0] + " fixture",
                    () -> {
                      DcaArchitecture arch = arch((String) pair[0], (FrameworkAnnotations) pair[1]);
                      assertDoesNotThrow(() -> DcaRules.checkAll(arch));
                    }));
  }

  @TestFactory
  @DisplayName("the Spring preset does not see Jakarta violations, the Jakarta preset does")
  Stream<DynamicTest> onlyTheMatchingPresetSeesTheViolation() {
    DcaArchitecture jakarta = arch("bad", FrameworkAnnotations.jakarta());
    DcaArchitecture spring = arch("bad", FrameworkAnnotations.spring());
    DcaArchitecture none = arch("bad", FrameworkAnnotations.none());
    return Stream.of(
        DynamicTest.dynamicTest(
            "DCA-ONI-003 reports @ApplicationScoped and @Entity under jakarta()",
            () -> {
              AssertionError error =
                  assertThrows(
                      AssertionError.class, () -> rule(jakarta, "DCA-ONI-003").check(jakarta));
              assertTrue(error.getMessage().contains("Invoice"), error.getMessage());
              assertTrue(
                  !error.getMessage().contains("Ledger"),
                  "Micronaut Data's @MappedEntity is not in the Jakarta preset: "
                      + error.getMessage());
            }),
        DynamicTest.dynamicTest(
            "DCA-ONI-003 reports @MappedEntity under micronaut()",
            () -> {
              DcaArchitecture micronaut = arch("bad", FrameworkAnnotations.micronaut());
              AssertionError error =
                  assertThrows(
                      AssertionError.class, () -> rule(micronaut, "DCA-ONI-003").check(micronaut));
              assertTrue(error.getMessage().contains("Ledger"), error.getMessage());
            }),
        DynamicTest.dynamicTest(
            "DCA-ONI-003 sees only the JPA entity under spring()",
            () -> {
              AssertionError error =
                  assertThrows(
                      AssertionError.class, () -> rule(spring, "DCA-ONI-003").check(spring));
              assertTrue(
                  error.getMessage().contains("jakarta.persistence.Entity"), error.getMessage());
              assertTrue(
                  !error.getMessage().contains("ApplicationScoped"),
                  "CDI's scope is not a Spring stereotype: " + error.getMessage());
            }),
        DynamicTest.dynamicTest(
            "DCA-ONI-003 has nothing to forbid under none()",
            () -> assertDoesNotThrow(() -> rule(none, "DCA-ONI-003").check(none))),
        DynamicTest.dynamicTest(
            "DCA-NAM-002 accepts configuration wiring under jakarta()",
            () -> assertDoesNotThrow(() -> rule(jakarta, "DCA-NAM-002").check(jakarta))),
        DynamicTest.dynamicTest(
            "DCA-NAM-002 selects nothing under none()",
            () -> assertDoesNotThrow(() -> rule(none, "DCA-NAM-002").check(none))),
        DynamicTest.dynamicTest(
            "DCA-NAM-006 finds the JAX-RS resource under jakarta(), not under spring()",
            () -> {
              AssertionError error =
                  assertThrows(
                      AssertionError.class, () -> rule(jakarta, "DCA-NAM-006").check(jakarta));
              assertTrue(error.getMessage().contains("InvoiceEndpoint"), error.getMessage());
              assertDoesNotThrow(() -> rule(spring, "DCA-NAM-006").check(spring));
            }));
  }

  @TestFactory
  @DisplayName("a violation message names the configured annotation, not Spring's")
  Stream<DynamicTest> messagesUseTheConfiguredVocabulary() {
    // The transactions fixture is written with Spring's annotations; read under a preset that
    // knows a differently named transactional annotation, every publishing path is uncovered.
    String pkg = Fixtures.ROOT + ".transactions";
    FrameworkAnnotations custom =
        FrameworkAnnotations.none().withTransactional("com.acme.platform.Atomic").named("acme");
    DcaArchitecture arch =
        DcaArchitecture.of(
            DcaLayout.forBasePackage(pkg).withFrameworkAnnotations(custom),
            new ClassFileImporter().importPackages(pkg));
    return Stream.of(
        DynamicTest.dynamicTest(
            "DCA-USE-012 says @Atomic",
            () -> {
              AssertionError error =
                  assertThrows(AssertionError.class, () -> rule(arch, "DCA-USE-012").check(arch));
              assertTrue(error.getMessage().contains("without @Atomic"), error.getMessage());
              assertTrue(!error.getMessage().contains("@Transactional"), error.getMessage());
            }),
        DynamicTest.dynamicTest(
            "the layout names the preset for the report",
            () -> assertTrue(arch.layout().toString().contains("frameworkAnnotations=acme"))));
  }

  @TestFactory
  @DisplayName("the presets know the annotations the fixtures use")
  Stream<DynamicTest> presetsCarryTheExpectedNames() {
    return Stream.of(
        DynamicTest.dynamicTest(
            "jakarta: CDI scopes, JAX-RS, JTA, JPA, no module system",
            () -> {
              FrameworkAnnotations j = FrameworkAnnotations.jakarta();
              assertTrue(j.injectable().contains("jakarta.enterprise.context.ApplicationScoped"));
              assertTrue(j.restController().equals(List.of("jakarta.ws.rs.Path")));
              assertTrue(j.transactional().equals(List.of("jakarta.transaction.Transactional")));
              assertTrue(j.persistenceEntity().contains("jakarta.persistence.Entity"));
              assertTrue(j.moduleDeclaration().isEmpty() && !j.hasModuleDeclaration());
            }),
        DynamicTest.dynamicTest(
            "quarkus is jakarta plus the event bus, minus a web-controller stereotype",
            () -> {
              FrameworkAnnotations q = FrameworkAnnotations.quarkus();
              assertTrue(q.injectable().equals(FrameworkAnnotations.jakarta().injectable()));
              assertTrue(q.eventListener().contains("io.quarkus.vertx.ConsumeEvent"));
              assertTrue(q.webController().isEmpty());
              assertTrue(q.name().equals("quarkus"));
            }),
        DynamicTest.dynamicTest(
            "spring is the default of DcaLayout and keeps the 0.3 names",
            () -> {
              FrameworkAnnotations s = DcaLayout.forBasePackage("com.acme").frameworkAnnotations();
              assertTrue(s.name().equals("spring"));
              assertTrue(s.injectable().contains("org.springframework.stereotype.Service"));
              assertTrue(s.injectable().contains("org.springframework.stereotype.Component"));
              assertTrue(
                  s.moduleDeclaration().contains("org.springframework.modulith.ApplicationModule"));
              assertTrue(
                  s.publishedInterface().contains("org.springframework.modulith.NamedInterface"));
              assertTrue(
                  s.transactional().contains("jakarta.transaction.Transactional"),
                  "Spring honours JTA's annotation too");
            }),
        DynamicTest.dynamicTest(
            "spring: a use case with JTA's @Transactional satisfies DCA-USE-012",
            () -> {
              // the jakarta fixture publishes under jakarta.transaction.Transactional only
              DcaArchitecture arch = arch("jakarta", FrameworkAnnotations.spring());
              assertDoesNotThrow(() -> rule(arch, "DCA-USE-012").check(arch));
            }));
  }
}
