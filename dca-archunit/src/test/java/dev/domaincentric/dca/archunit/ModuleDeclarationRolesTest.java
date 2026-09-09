package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.contextmap.ContextMapRenderer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The module-declaration and published-interface roles may hold several annotations; what counts is
 * the annotation a package actually carries, not the first one on the class path.
 */
class ModuleDeclarationRolesTest {

  private static final String PKG = "dev.domaincentric.dca.archunit.fixtures.frameworks.modules";
  private static final String SECOND_MODULE = "org.example.modules.Module";
  private static final String SECOND_PUBLISHED = "org.example.modules.PublishedPackage";

  private static DcaArchitecture arch(FrameworkAnnotations annotations) {
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(PKG).withFrameworkAnnotations(annotations),
        new ClassFileImporter().importPackages(PKG));
  }

  private static DcaRule map006(DcaArchitecture arch) {
    return DcaRules.all(arch.layout()).stream()
        .filter(r -> r.id().equals("DCA-MAP-006"))
        .findFirst()
        .orElseThrow();
  }

  @Test
  @DisplayName(
      "DCA-MAP-006 reads the allowed dependencies from the declaration the package carries")
  void secondModuleDeclarationCounts() {
    // Spring Modulith's annotation is loadable (test shim) but no package here carries it
    FrameworkAnnotations both =
        FrameworkAnnotations.spring()
            .withModuleDeclaration("org.springframework.modulith.ApplicationModule", SECOND_MODULE);
    DcaArchitecture arch = arch(both);
    assertDoesNotThrow(() -> map006(arch).check(arch));
  }

  @Test
  @DisplayName("a declaration the layout does not configure is not consulted")
  void unconfiguredDeclarationIsIgnored() {
    DcaArchitecture arch = arch(FrameworkAnnotations.spring());
    AssertionError error = assertThrows(AssertionError.class, () -> map006(arch).check(arch));
    assertTrue(error.getMessage().contains("billing"), error.getMessage());
  }

  @Test
  @DisplayName(
      "the renderer accepts a published-interface declaration from any configured annotation")
  void secondPublishedInterfaceDeclarationCounts() {
    FrameworkAnnotations both =
        FrameworkAnnotations.spring()
            .withPublishedInterface(
                "org.springframework.modulith.NamedInterface", SECOND_PUBLISHED);
    String md = ContextMapRenderer.of(arch(both)).render();
    assertTrue(md.contains("| ledger | Ledger | Postings | api |"), md);
    assertTrue(
        md.contains("| billing | Billing | Invoices | — |"),
        "classes alone do not publish once a declaration is configured and loadable: " + md);
  }

  @Test
  @DisplayName("without a loadable published-interface declaration class presence stands alone")
  void withoutLoadableDeclarationClassPresenceStandsAlone() {
    // NamedInterface has no test shim, so the Spring preset's only declaration is not loadable
    String md = ContextMapRenderer.of(arch(FrameworkAnnotations.spring())).render();
    assertTrue(md.contains("| billing | Billing | Invoices | api |"), md);
  }
}
