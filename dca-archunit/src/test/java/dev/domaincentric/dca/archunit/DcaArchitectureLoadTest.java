package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** What {@link DcaArchitecture#load} imports, and what it refuses to stay silent about. */
class DcaArchitectureLoadTest {

  /**
   * A package that reaches the class path only inside a jar — the shape a sibling module takes in a
   * multi-module build, where the contexts live in modules other than the one that runs the
   * architecture test.
   */
  private static final String PACKAGE_IN_A_JAR = "com.tngtech.archunit.lang";

  @Test
  void importsClassesThatReachTheClassPathAsAJar() {
    DcaArchitecture architecture = DcaArchitecture.load(DcaLayout.forBasePackage(PACKAGE_IN_A_JAR));

    assertTrue(architecture.classes().iterator().hasNext());
  }

  @Test
  void customImportOptionsReplaceTheDefaults() {
    IllegalStateException empty =
        assertThrows(
            IllegalStateException.class,
            () ->
                DcaArchitecture.load(
                    DcaLayout.forBasePackage(PACKAGE_IN_A_JAR),
                    ImportOption.Predefined.DO_NOT_INCLUDE_JARS));

    assertTrue(empty.getMessage().contains(PACKAGE_IN_A_JAR), empty.getMessage());
  }

  @Test
  void refusesAnImportThatFoundNothing() {
    IllegalStateException empty =
        assertThrows(
            IllegalStateException.class,
            () -> DcaArchitecture.load(DcaLayout.forBasePackage("com.example.absent")));

    assertTrue(empty.getMessage().contains("multi-module"), empty.getMessage());
  }

  @Test
  void wrappingImportedClassesStaysUnchecked() {
    DcaArchitecture architecture =
        DcaArchitecture.of(
            DcaLayout.forBasePackage("com.example.absent"),
            new com.tngtech.archunit.core.importer.ClassFileImporter()
                .importPackages("com.example.absent"));

    assertEquals(0, architecture.classes().size());
  }
}
