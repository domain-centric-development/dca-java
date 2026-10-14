package dev.domaincentric.dca.archunit.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSelection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.opentest4j.TestAbortedException;

/**
 * An experimental rule in scope is listed in a container of its own, named {@code experimental},
 * and the summary line counts it apart from the released rules. With the opt-in it runs there;
 * without it, it is reported as aborted with the reason that names the opt-in. The rule is a
 * test-only fixture.
 */
class DcaArchitectureTestExperimentalTest {

  private static final DcaRule EXPERIMENTAL =
      DcaRule.check("DCA-USE-900", "Fixture experimental rule", "Exists for the test", arch -> {})
          .experimental()
          .selecting("Nothing; a fixture.")
          .checking("Nothing; a fixture.");

  private static final DcaRule RELEASED =
      DcaRule.check("DCA-USE-901", "Fixture released rule", "Exists for the test", arch -> {})
          .selecting("Nothing; a fixture.")
          .checking("Nothing; a fixture.");

  /** The two fixtures, filed in the use-case set, in scope of the selection it is given. */
  private static final class Fixture extends DcaArchitectureTest {
    private final DcaRuleSelection additional;

    Fixture(DcaRuleSelection additional) {
      this.additional = additional;
    }

    @Override
    protected DcaLayout layout() {
      return DcaLayout.forBasePackage("com.acme.shop");
    }

    /** The fixtures read no class, so the import is this one class rather than a package. */
    @Override
    protected DcaArchitecture architecture() {
      return DcaArchitecture.of(
          effectiveLayout(), new ClassFileImporter().importClasses(Fixture.class));
    }

    @Override
    protected DcaRuleSelection additionalSelection() {
      return additional;
    }

    @Override
    protected List<DcaRule> rules() {
      DcaRuleSelection selection = selection();
      return List.of(RELEASED, EXPERIMENTAL).stream()
          .filter(rule -> selection.includes("usecase", rule.id()))
          .toList();
    }
  }

  private static Map<String, List<String>> containers(DcaArchitectureTest test) {
    return test.dcaRules()
        .filter(DynamicContainer.class::isInstance)
        .map(DynamicContainer.class::cast)
        .collect(
            Collectors.toMap(
                DynamicNode::getDisplayName,
                container -> container.getChildren().map(DynamicNode::getDisplayName).toList()));
  }

  private static List<String> topLevelNames(DcaArchitectureTest test) {
    return test.dcaRules().map(DynamicNode::getDisplayName).toList();
  }

  @Test
  void anOptedInExperimentalRuleRunsInTheExperimentalContainer() {
    Fixture test = new Fixture(DcaRuleSelection.all().withExperimental());

    Map<String, List<String>> containers = containers(test);

    assertEquals(
        List.of("[DCA-USE-900] Fixture experimental rule"), containers.get("experimental (1)"));
    assertEquals(
        List.of("[DCA-USE-901] Fixture released rule"),
        containers.get("custom (1)"),
        "the released rule stays in its set's container");
    assertTrue(topLevelNames(test).contains("1 enforced, 0 informational, 1 experimental"));
  }

  @Test
  void withoutTheOptInAnExperimentalRuleIsListedAsNotRun() {
    Fixture test = new Fixture(DcaRuleSelection.all());

    Map<String, List<String>> containers = containers(test);

    assertEquals(
        List.of("[DCA-USE-900] Fixture experimental rule"), containers.get("experimental (1)"));
    assertEquals(List.of("[DCA-USE-901] Fixture released rule"), containers.get("custom (1)"));
    assertTrue(topLevelNames(test).contains("1 enforced, 0 informational, 1 experimental"));
    TestAbortedException aborted =
        assertThrows(TestAbortedException.class, () -> experimentalTest(test).execute());
    assertEquals(
        "not run — experimental; opt in with dca.rules.experimental = true or withExperimental()",
        aborted.getMessage());
  }

  @Test
  void withTheOptInAnExperimentalRuleRuns() throws Throwable {
    Fixture test = new Fixture(DcaRuleSelection.all().withExperimental());

    experimentalTest(test).execute(); // the fixture passes, so a run completes without abort
  }

  private static org.junit.jupiter.api.function.Executable experimentalTest(
      DcaArchitectureTest test) {
    DynamicContainer container =
        test.dcaRules()
            .filter(DynamicContainer.class::isInstance)
            .map(DynamicContainer.class::cast)
            .filter(node -> node.getDisplayName().equals("experimental (1)"))
            .findFirst()
            .orElseThrow();
    DynamicTest only = (DynamicTest) container.getChildren().findFirst().orElseThrow();
    return only.getExecutable();
  }
}
