package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Package cycle detection: no circular dependencies between the per-context slices of one layer
 * (domain model, application, incoming adapters, outgoing adapters), and none between the feature
 * or use-case slices inside one module's application layer.
 *
 * <p>Reference: Clean Architecture, Acyclic Dependencies Principle (ADP).
 */
public final class CycleRules implements DcaRuleSet {

  private final List<DcaRule> rules;

  public CycleRules(DcaLayout layout) {
    this.rules = List.of(applicationSlicesFreeOfCycles(layout));
  }

  @Override
  public String name() {
    return "cycles";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  public static DcaRule applicationSlicesFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
            "DCA-CYC-005",
            "Feature and use case packages within a module's application layer must not have cyclic"
                + " dependencies",
            "The packages directly below a module's application package are its features"
                + " (application.<feature>.<usecase>) or, in a flat layout, its use cases"
                + " (application.<usecase>). A feature is an optional, domain-named group of related"
                + " use cases; it may depend on another feature in one direction, but a cycle between"
                + " two of them means the grouping does not carry its weight - the shared concept"
                + " belongs in application.shared, in the domain, or in one of the two. application.shared"
                + " is the context-wide port package and is not a slice. The rule does not infer bounded"
                + " contexts or aggregate ownership from the packages it slices",
            arch ->
                slices()
                    .assignedFrom(applicationChildSlices(arch, layout))
                    .should()
                    .beFreeOfCycles()
                    .allowEmptyShould(true))
        .selecting(
            "One slice per operation-root package (marker or suffix), with configured containers stripped; supporting subfolders join the nearest operation root. Classes directly in an enclosing feature package form its feature slice. Shared and direct application classes are ignored.")
        .checking(
            "The slices form no dependency cycle: two features or two use cases that depend on"
                + " each other, directly or through further slices, are reported. Dependencies on"
                + " application.shared, the domain or an adapter do not count. Slices of all"
                + " modules are checked together, so a cycle through another module's use case"
                + " package is"
                + " reported here as well.");
  }

  /**
   * One slice per immediate child package of a module's application package — the module root comes
   * from {@link DcaArchitecture#moduleRootOf(String)}, so the slicing holds at any depth and never
   * assumes a module is a direct child of the base package. Classes directly in the application
   * package and everything below {@code application.shared} are ignored.
   */
  private static SliceAssignment applicationChildSlices(DcaArchitecture arch, DcaLayout layout) {
    return new SliceAssignment() {

      @Override
      public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
        String root = arch.moduleRootOf(javaClass.getPackageName());
        if (root == null) {
          return SliceIdentifier.ignore();
        }
        String application = root + "." + layout.applicationSubpackage();
        String pkg = javaClass.getPackageName();
        if (!pkg.startsWith(application + ".")) {
          return SliceIdentifier.ignore();
        }
        String relative = pkg.substring(application.length() + 1);
        String[] segments = relative.split("\\.");
        int index = 0;
        while (index < segments.length && layout.operationContainers().contains(segments[index]))
          index++;
        if (index == segments.length || segments[index].equals("shared"))
          return SliceIdentifier.ignore();
        String operationRoot =
            arch.classes().stream()
                .filter(c -> OperationPolicy.operation(c, arch))
                .map(JavaClass::getPackageName)
                .filter(
                    p ->
                        p.startsWith(application + ".")
                            && (pkg.equals(p) || pkg.startsWith(p + ".")))
                .max(java.util.Comparator.comparingInt(String::length))
                .orElse(null);
        String physicalFeature =
            application
                + "."
                + String.join(".", java.util.Arrays.copyOfRange(segments, 0, index + 1));
        if (operationRoot == null) {
          boolean feature =
              arch.classes().stream()
                  .anyMatch(
                      c ->
                          OperationPolicy.operation(c, arch)
                              && c.getPackageName().startsWith(physicalFeature + "."));
          if (!feature) return SliceIdentifier.ignore();
          operationRoot = physicalFeature;
        }
        String logical =
            java.util.Arrays.stream(operationRoot.substring(application.length() + 1).split("\\."))
                .filter(segment -> !layout.operationContainers().contains(segment))
                .collect(java.util.stream.Collectors.joining("."));
        return SliceIdentifier.of(application + "." + logical);
      }

      @Override
      public String getDescription() {
        return "feature or use case packages";
      }
    };
  }

  /**
   * One slice per module, holding that module's classes in the layer {@code layerOf} names.
   *
   * <p>Replaces {@code slices().matching(base + ".(*)." + layer + "..")}. A slice pattern needs a
   * capture group to derive the slice identity, and {@code (*)} is exactly one segment — so the
   * matching form only ever sliced modules that were direct children of the base package, and a
   * grouped or nested one was silently excluded from the cycle check. Assigning slices explicitly
   * uses {@link DcaArchitecture#moduleRootOf(String)} and therefore holds at any depth.
   */
  private static SliceAssignment moduleLayerSlices(
      DcaArchitecture arch, UnaryOperator<String> layerOf) {
    return new SliceAssignment() {

      @Override
      public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
        String root = arch.moduleRootOf(javaClass.getPackageName());
        if (root == null) {
          return SliceIdentifier.ignore();
        }
        String layer = layerOf.apply(root);
        String pkg = javaClass.getPackageName();
        return pkg.equals(layer) || pkg.startsWith(layer + ".")
            ? SliceIdentifier.of(root)
            : SliceIdentifier.ignore();
      }

      @Override
      public String getDescription() {
        return "modules";
      }
    };
  }
}
