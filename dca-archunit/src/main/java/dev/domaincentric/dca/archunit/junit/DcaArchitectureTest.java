package dev.domaincentric.dca.archunit.junit;

import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleExecution;
import dev.domaincentric.dca.archunit.DcaRuleOutcome;
import dev.domaincentric.dca.archunit.DcaRuleSelection;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRules;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * JUnit 5 base class that runs the DCA rule catalog as dynamic tests — one container per rule set,
 * one test per rule, named {@code [DCA-TAC-001] Aggregate roots must …}.
 *
 * <pre>{@code
 * class ArchitectureTest extends DcaArchitectureTest {
 *   @Override
 *   protected DcaLayout layout() {
 *     return DcaLayout.forBasePackage("com.acme.shop");
 *   }
 * }
 * }</pre>
 *
 * <p>The first container, {@code layout}, holds one always-passing test that names the framework
 * preset the rules resolved and how it was chosen ({@code framework annotations: spring
 * (detected)}, {@code quarkus (detected; also jakarta)}, {@code spring (default)}, {@code spring
 * (default; undecided: micronaut, quarkus)} for a mixed class path, {@code acme (configured)},
 * {@code jakarta (explicit)}) — so a report of a Jakarta or hand-wired project shows which
 * vocabulary the rules used, and a wrong default is visible instead of silently selecting nothing.
 * {@code dca.framework=<name>} in {@code dca-archunit.properties} selects a preset by name
 * (built-in or contributed through {@code FrameworkAnnotationsProvider}) unless {@link #layout()}
 * already set one explicitly in code.
 *
 * <p>Unless {@link #selection()} is overridden, the configuration comes from {@code
 * dca-archunit.properties} on the test class path — see {@link DcaRuleSelection#fromClasspath()}.
 * Rules that were lowered to {@link dev.domaincentric.dca.archunit.DcaSeverity#WARN} or switched
 * off are reported as aborted tests carrying their recorded reason, so a decision to skip a rule
 * stays visible instead of disappearing from the report.
 *
 * <p>Experimental rules in scope are listed in a container of their own, {@code experimental},
 * after the rule sets, and the summary line counts them apart from the released ones ({@code 120
 * enforced, 4 informational, 2 experimental}). They run only where the selection opts in ({@link
 * DcaRuleSelection#withExperimental()}, {@code dca.rules.experimental=true}); without it each is
 * reported as an aborted test that names the opt-in, the way a switched-off rule is.
 *
 * <p>Requires JUnit Jupiter on the test class path (it is a {@code compileOnly} dependency of this
 * library).
 */
public abstract class DcaArchitectureTest {

  /** Name of the container the experimental rules run in, beside the rule-set containers. */
  public static final String EXPERIMENTAL_CONTAINER = "experimental";

  /** Why an experimental rule in scope did not run: the selection did not opt in. */
  public static final String EXPERIMENTAL_NOT_RUN =
      "not run — experimental; opt in with dca.rules.experimental = true or withExperimental()";

  private DcaArchitecture architecture;

  /** The layout of the project under test. */
  protected abstract DcaLayout layout();

  /**
   * Which rules run and how strictly: {@code dca-archunit.properties} from the class path, with
   * {@link #additionalSelection()} applied on top.
   *
   * <p>Override {@link #additionalSelection()} to configure rules in code — overriding this method
   * replaces the properties file instead of adding to it.
   */
  protected DcaRuleSelection selection() {
    return DcaRuleSelection.fromClasspath()
        .mergedWith(fromLegacyOverrides())
        .mergedWith(additionalSelection());
  }

  /**
   * Project-specific configuration applied on top of {@code dca-archunit.properties}. Default:
   * none, so the file alone decides.
   *
   * <pre>{@code
   * @Override
   * protected DcaRuleSelection additionalSelection() {
   *   return DcaRuleSelection.all().warning("DCA-TAC-009", "being made final");
   * }
   * }</pre>
   */
  protected DcaRuleSelection additionalSelection() {
    return DcaRuleSelection.all();
  }

  /** Identifiers of rules to skip. Default: none. Prefer {@link #selection()}. */
  protected Set<String> excludedRuleIds() {
    return Set.of();
  }

  /**
   * The rules in scope. Default: whatever {@link #selection()} asks for, with the experimental
   * rules in its scope added — they run only where the selection opts in, and are reported as not
   * run otherwise.
   */
  protected List<DcaRule> rules() {
    return DcaRules.selectFlat(effectiveLayout(), selection().withExperimental());
  }

  /** The imported architecture; loaded once per test instance. */
  protected DcaArchitecture architecture() {
    if (architecture == null) {
      architecture = DcaArchitecture.load(effectiveLayout());
    }
    return architecture;
  }

  /**
   * {@link #layout()} with {@code dca.framework=<name>} from {@code dca-archunit.properties}
   * applied — unless the layout already names its framework annotations explicitly, which always
   * wins.
   */
  protected DcaLayout effectiveLayout() {
    return applyConfiguredFramework(layout(), classpathProperties());
  }

  static DcaLayout applyConfiguredFramework(DcaLayout layout, Properties properties) {
    String name = properties.getProperty("dca.framework");
    if (name == null || name.isBlank()) {
      return layout;
    }
    if (layout.frameworkAnnotationsOrigin() == DcaLayout.FrameworkAnnotationsOrigin.EXPLICIT) {
      return layout;
    }
    return layout.withFrameworkPreset(name.trim());
  }

  private static Properties classpathProperties() {
    Properties properties = new Properties();
    ClassLoader loader = Thread.currentThread().getContextClassLoader();
    if (loader == null) {
      loader = DcaArchitectureTest.class.getClassLoader();
    }
    try (InputStream in = loader.getResourceAsStream(DcaRuleSelection.DEFAULT_RESOURCE)) {
      if (in != null) {
        properties.load(in);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(
          "Cannot read " + DcaRuleSelection.DEFAULT_RESOURCE + " from the class path", e);
    }
    return properties;
  }

  @TestFactory
  Stream<DynamicNode> dcaRules() {
    DcaArchitecture arch = architecture();
    DcaRuleSelection selection = selection();
    var selectedRules = rules();
    List<DcaRule> released =
        selectedRules.stream().filter(r -> r.maturity() == DcaRule.Maturity.RELEASED).toList();
    List<DcaRule> experimental =
        selectedRules.stream().filter(r -> r.maturity() == DcaRule.Maturity.EXPERIMENTAL).toList();
    long informational =
        released.stream().filter(r -> r.kind() == DcaRule.Kind.INFORMATIONAL).count();
    String counts =
        (released.size() - informational)
            + " enforced, "
            + informational
            + " informational, "
            + experimental.size()
            + " experimental";
    System.out.println(counts);
    selection.retirementNotices().forEach(System.out::println);
    Map<String, List<DcaRule>> containers = new LinkedHashMap<>(groupBySet(released));
    if (!experimental.isEmpty()) {
      containers.put(EXPERIMENTAL_CONTAINER, experimental);
    }
    Stream<DynamicNode> sets =
        containers.entrySet().stream()
            .map(
                entry ->
                    DynamicContainer.dynamicContainer(
                        entry.getKey() + " (" + entry.getValue().size() + ")",
                        entry.getValue().stream()
                            .map(rule -> test(rule, arch, selection))
                            .toList()));
    return Stream.concat(
        Stream.of(
            layoutDiagnostics(arch),
            DynamicTest.dynamicTest(counts, () -> {}),
            DynamicContainer.dynamicContainer(
                "retired identities referenced by the selection ("
                    + selection.referencedRetiredIds().size()
                    + ")",
                selection.retirementNotices().stream()
                    .map(notice -> DynamicTest.dynamicTest(notice, () -> {})))),
        sets);
  }

  /**
   * One passing test per fact of the layout worth seeing in the report: the framework preset the
   * rules resolved their annotations with, and the vocabulary they resolved their building blocks
   * with.
   */
  private DynamicContainer layoutDiagnostics(DcaArchitecture arch) {
    DcaLayout layout = arch.layout();
    List<DynamicTest> notices = new ArrayList<>();
    if (!layout.excludedPackages().isEmpty()) {
      notices.add(
          DynamicTest.dynamicTest(
              "excluded packages: " + String.join(", ", layout.excludedPackages()),
              () -> {
                // diagnostic only - what the architecture under test leaves out
              }));
    }
    arch.baseModuleRootNotice()
        .ifPresent(
            notice -> {
              System.out.println("DCA layout: " + notice);
              notices.add(
                  DynamicTest.dynamicTest(
                      "module roots: " + notice,
                      () -> {
                        // diagnostic only - the rules still run, but every module counts as a
                        // neighbour of every other
                      }));
            });
    for (String notice : arch.layerNotices()) {
      System.out.println("DCA layout: " + notice);
      notices.add(
          DynamicTest.dynamicTest(
              "layers: " + notice,
              () -> {
                // diagnostic only - the annotation holds; the notice names where it overrides
              }));
    }
    return DynamicContainer.dynamicContainer(
        "layout",
        Stream.concat(
            notices.stream(),
            Stream.of(
                DynamicTest.dynamicTest(
                    "framework annotations: " + layout.frameworkAnnotationsReport(),
                    () -> {
                      // diagnostic only - the preset in use, named in the report
                    }),
                DynamicTest.dynamicTest(
                    "layers: " + layout.layersReport(),
                    () -> {
                      // diagnostic only - how each layer is recognised, named in the report
                    }),
                DynamicTest.dynamicTest(
                    "building block markers: " + layout.markersReport(),
                    () -> {
                      // diagnostic only - the vocabulary the rules select on, named in the report
                    }))));
  }

  /** Groups the rules to run by the catalog set they belong to, keeping the catalog's order. */
  private Map<String, List<DcaRule>> groupBySet(List<DcaRule> rules) {
    Map<String, String> setOfRule = new LinkedHashMap<>();
    for (DcaRuleSet set : DcaRules.ruleSets(effectiveLayout())) {
      set.rules().forEach(rule -> setOfRule.put(rule.id(), set.name()));
    }
    Map<String, List<DcaRule>> bySet = new LinkedHashMap<>();
    DcaRules.setNames().forEach(name -> bySet.put(name, new ArrayList<>()));
    for (DcaRule rule : rules) {
      bySet
          .computeIfAbsent(setOfRule.getOrDefault(rule.id(), "custom"), key -> new ArrayList<>())
          .add(rule);
    }
    bySet.values().removeIf(List::isEmpty);
    return bySet;
  }

  private DynamicTest test(DcaRule rule, DcaArchitecture arch, DcaRuleSelection selection) {
    return DynamicTest.dynamicTest(
        "[" + rule.id() + "] " + rule.title(),
        () -> {
          if (rule.maturity() == DcaRule.Maturity.EXPERIMENTAL
              && !selection.includesExperimental()) {
            Assumptions.abort(EXPERIMENTAL_NOT_RUN);
          }
          DcaRuleOutcome outcome = DcaRuleExecution.execute(rule, arch, selection);
          switch (outcome.status()) {
            case FAILED -> throw new AssertionError(outcome.message());
            case WARNED -> Assumptions.abort("WARNING — " + outcome.message());
            case SKIPPED -> Assumptions.abort("switched off — " + outcome.message());
            case PASSED -> {
              // A diagnostic that observed something is reported the way a warning is: as an
              // aborted test carrying its text. JUnit shows no message for a passing test, and
              // stdout reaches no report and no pipeline stage.
              if (outcome.message() != null) {
                Assumptions.abort("INFO — " + outcome.message());
              }
            }
          }
        });
  }

  /** Bridges the older {@link #excludedRuleIds()} hook onto the selection. */
  private DcaRuleSelection fromLegacyOverrides() {
    DcaRuleSelection legacy = DcaRuleSelection.all();
    for (String id : excludedRuleIds()) {
      legacy = legacy.excluding(id);
    }
    return legacy;
  }
}
