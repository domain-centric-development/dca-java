package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The whole catalog against a project on its first day: one bounded context with a model and a use
 * case, no adapter, no infrastructure, no shared kernel. Every rule whose selection is empty here
 * must pass, not fail on the empty selection — otherwise a fresh project cannot adopt the catalog
 * before it has written its first controller.
 */
class GreenfieldTest {

  private static final String BASE = "dev.domaincentric.dca.archunit.fixtures.layout.greenfield";

  @Test
  @DisplayName("a context without adapters or infrastructure runs the full catalog green")
  void fullCatalogPassesWithoutAdapters() {
    DcaArchitecture arch =
        DcaArchitecture.of(
            DcaLayout.forBasePackage(BASE), new ClassFileImporter().importPackages(BASE));
    assertDoesNotThrow(() -> DcaRules.checkAll(arch));
  }
}
