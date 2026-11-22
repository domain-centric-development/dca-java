package dev.domaincentric.dca.archunit.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ImportOption;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

/**
 * The real entry point on a freshly generated application: a class annotated as the framework's
 * application that only calls the framework from {@code main}. The fixture lives in test output, so
 * the import leaves out jars only instead of test classes; everything else is the default run.
 */
class DcaArchitectureTestEntryPointTest {

  private static final String ROOT = "dev.domaincentric.dca.archunit.fixtures.entrypoint";

  private static final class Generated extends DcaArchitectureTest {
    private final String basePackage;

    Generated(String basePackage) {
      this.basePackage = basePackage;
    }

    @Override
    protected DcaLayout layout() {
      return DcaLayout.forBasePackage(basePackage);
    }

    @Override
    protected DcaArchitecture architecture() {
      return DcaArchitecture.load(effectiveLayout(), ImportOption.Predefined.DO_NOT_INCLUDE_JARS);
    }
  }

  @Test
  void theWholeCatalogPassesOnAGeneratedApplication() throws Throwable {
    List<String> names = new ArrayList<>();
    List<String> failed = new ArrayList<>();
    run(new Generated(ROOT + ".only").dcaRules(), names, failed);

    assertTrue(failed.isEmpty(), "failed on a generated application: " + failed);
    assertTrue(names.size() > 100, "ran only " + names.size() + " tests");
    assertTrue(
        names.stream().anyMatch(n -> n.startsWith("production code: ")),
        "no notice why the suite is green");
  }

  @Test
  void oneRealTypeBesideTheEntryPointTurnsTheGuardsRed() throws Throwable {
    List<String> names = new ArrayList<>();
    List<String> failed = new ArrayList<>();
    run(new Generated(ROOT + ".plus").dcaRules(), names, failed);

    List<String> guards =
        failed.stream()
            .filter(f -> f.contains("DCA-STR-011") || f.contains("DCA-STR-012"))
            .toList();
    assertEquals(2, guards.size(), "failed: " + failed);
    assertTrue(names.stream().noneMatch(n -> n.startsWith("production code: ")));
  }

  private static void run(
      Stream<? extends DynamicNode> nodes, List<String> names, List<String> failed) {
    for (DynamicNode node : nodes.toList()) {
      if (node instanceof DynamicContainer container) {
        run(container.getChildren(), names, failed);
      } else if (node instanceof DynamicTest test) {
        names.add(test.getDisplayName());
        try {
          test.getExecutable().execute();
        } catch (TestAbortedException notRun) {
          // an experimental rule outside the opt-in: reported as not run, not as failed
        } catch (Throwable failure) {
          failed.add(test.getDisplayName() + ": " + failure.getMessage());
        }
      }
    }
  }
}
