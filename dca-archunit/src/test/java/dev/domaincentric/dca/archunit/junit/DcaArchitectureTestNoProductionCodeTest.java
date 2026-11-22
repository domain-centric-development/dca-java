package dev.domaincentric.dca.archunit.junit;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * The real entry point on a project without production code: the default import of {@link
 * DcaArchitectureTest} over a base package that holds no class, on a class path whose own output
 * holds test classes and the DCA libraries only. Every test of the whole catalog passes, and the
 * report says why.
 */
class DcaArchitectureTestNoProductionCodeTest {

  private static final class EmptyProject extends DcaArchitectureTest {
    @Override
    protected DcaLayout layout() {
      return DcaLayout.forBasePackage("com.example.noproductioncode");
    }
  }

  @Test
  void theWholeCatalogPassesOnAProjectWithoutProductionCode() throws Throwable {
    List<String> names = new ArrayList<>();
    List<String> failed = new ArrayList<>();
    run(new EmptyProject().dcaRules(), names, failed);

    assertTrue(failed.isEmpty(), "failed on a project without production code: " + failed);
    assertTrue(names.size() > 100, "ran only " + names.size() + " tests");
    assertTrue(
        names.stream().anyMatch(n -> n.startsWith("production code: ")),
        "no notice why the suite is green");
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
