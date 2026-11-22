package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
                    classPathWithProductionCode(),
                    ImportOption.Predefined.DO_NOT_INCLUDE_JARS));

    assertTrue(empty.getMessage().contains(PACKAGE_IN_A_JAR), empty.getMessage());
  }

  /**
   * A misspelt base package over a project that has production code: the project's own output holds
   * a class outside the base package, so the import is refused and names it.
   */
  @Test
  void refusesAnImportThatFoundNothingWhileTheProjectHasCode() {
    IllegalStateException empty =
        assertThrows(
            IllegalStateException.class,
            () ->
                DcaArchitecture.load(
                    DcaLayout.forBasePackage("com.example.absent"),
                    classPathWithProductionCode(),
                    ImportOption.Predefined.DO_NOT_INCLUDE_TESTS));

    assertTrue(empty.getMessage().contains("multi-module"), empty.getMessage());
    assertTrue(empty.getMessage().contains(PRODUCTION_CLASS.getName()), empty.getMessage());
  }

  /**
   * A project without production code: nothing below the base package and nothing in the project's
   * own output - this test class path holds only test output, the DCA libraries and jars. The
   * import passes and says why.
   */
  @Test
  void acceptsAProjectWithoutProductionCode() {
    DcaArchitecture architecture =
        DcaArchitecture.load(DcaLayout.forBasePackage("com.example.absent"));

    assertEquals(0, architecture.classes().size());
    assertTrue(architecture.noProductionCodeNotice().isPresent());
    assertTrue(
        architecture.noProductionCodeNotice().get().contains("without production code"),
        architecture.noProductionCodeNotice().get());
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

  /** The shapes of an entry point: only main, annotations and a lambda inside main allowed. */
  @Test
  void anEntryPointOnlyStartsTheProgram() {
    String shapes = Fixtures.ROOT + ".entrypoint.shapes";
    com.tngtech.archunit.core.domain.JavaClasses classes =
        new com.tngtech.archunit.core.importer.ClassFileImporter()
            .importPackages(Fixtures.ROOT + ".entrypoint");
    java.util.function.Predicate<String> entryPoint =
        name -> DcaArchitecture.isEntryPoint(classes.get(name), classes);

    assertTrue(entryPoint.test(Fixtures.ROOT + ".entrypoint.only.Application"));
    assertTrue(entryPoint.test(shapes + ".WithLambda"));
    for (String production :
        List.of("WithField", "WithSecondMethod", "WithNestedType", "WithoutMain")) {
      assertTrue(!entryPoint.test(shapes + "." + production), production);
    }
    assertTrue(!entryPoint.test(Fixtures.ROOT + ".entrypoint.plus.Thing"));
  }

  /** A class outside every DCA package, standing in for the project's own production code. */
  private static final Class<?> PRODUCTION_CLASS = com.tngtech.archunit.lang.ArchRule.class;

  @TempDir static Path output;

  /** The test class path plus a directory that holds one production class - a project's output. */
  private static Set<Location> classPathWithProductionCode() throws IOException {
    String file = PRODUCTION_CLASS.getName().replace('.', '/') + ".class";
    Path target = output.resolve("classes").resolve(file);
    Files.createDirectories(target.getParent());
    try (InputStream in = PRODUCTION_CLASS.getClassLoader().getResourceAsStream(file)) {
      Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
    }
    Set<Location> classPath = new HashSet<>(Locations.inClassPath());
    classPath.add(Location.of(output.resolve("classes")));
    return classPath;
  }
}
