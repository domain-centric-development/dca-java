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
    this.rules =
        List.of(
            domainPackagesFreeOfCycles(layout),
            applicationLayerFreeOfCycles(layout),
            outgoingAdaptersFreeOfCycles(layout),
            incomingAdaptersFreeOfCycles(layout),
            applicationSlicesFreeOfCycles(layout));
  }

  @Override
  public String name() {
    return "cycles";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  public static DcaRule domainPackagesFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
            "DCA-CYC-001",
            "Domain Packages must not have cyclic dependencies (package-based slice discovery)",
            "Domain model packages should have clear boundaries and no cycles (Acyclic Dependencies"
                + " Principle)",
            arch ->
                slices()
                    .assignedFrom(moduleLayerSlices(arch, layout::domainModelPackage))
                    .should()
                    .beFreeOfCycles()
                    .allowEmptyShould(true))
        .selecting(
            "One slice per module root, holding the classes in <module>.domain.model.. of that"
                + " module (segment names from the layout). A module root is the shortest package"
                + " prefix whose next segment is a layer segment, so modules are found at any depth;"
                + " classes outside every module or outside the domain-model package are ignored.")
        .checking(
            "The slices form no dependency cycle - no two modules' domain models depend on each"
                + " other, directly or via further modules' domain models. Slices are per module"
                + " root, so a cycle between classes inside one module's domain model is not"
                + " detected here, and dependencies into other layers do not count. DCA-CYC-005"
                + " covers the application layer per operation; no rule slices the domain model"
                + " within a module. Fewer than two slices pass.");
  }

  public static DcaRule applicationLayerFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
            "DCA-CYC-002",
            "Application Layer must not have cyclic dependencies (package-based slice discovery)",
            "Application services should have clear boundaries and no cycles",
            arch ->
                slices()
                    .assignedFrom(
                        moduleLayerSlices(
                            arch, root -> root + "." + layout.applicationSubpackage()))
                    .should()
                    .beFreeOfCycles()
                    .allowEmptyShould(true))
        .selecting(
            "One slice per module root, holding the classes in <module>.application.. of that"
                + " module (application.shared included); classes outside every module or outside"
                + " the application layer are ignored.")
        .checking(
            "The slices form no dependency cycle between modules' application layers. Slices are"
                + " per module root, so a cycle between use cases or features inside one module is"
                + " not detected here - DCA-CYC-005 covers the application layer per operation -"
                + " and dependencies into domain or adapter classes do not count.");
  }

  public static DcaRule outgoingAdaptersFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
            "DCA-CYC-003",
            "Outgoing Adapter Packages must not have cyclic dependencies",
            "Outgoing adapters should have clear boundaries and no cycles",
            arch ->
                slices()
                    .assignedFrom(
                        moduleLayerSlices(
                            arch,
                            root ->
                                root
                                    + "."
                                    + layout.adapterSubpackage()
                                    + "."
                                    + layout.outgoingSubpackage()))
                    .should()
                    .beFreeOfCycles()
                    .allowEmptyShould(true))
        .selecting(
            "One slice per module root, holding the classes in <module>.adapter.outgoing.. of"
                + " that module; everything else is ignored.")
        .checking(
            "The slices form no dependency cycle between modules' outgoing adapters. Slices are"
                + " per module root, so a cycle inside one module's outgoing adapters is not"
                + " detected here, and dependencies into other layers do not count. DCA-CYC-005"
                + " covers the application layer per operation; no rule slices the adapters within"
                + " a module.");
  }

  public static DcaRule incomingAdaptersFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
            "DCA-CYC-004",
            "Incoming Adapter Packages must not have cyclic dependencies",
            "Incoming adapters should have clear boundaries and no cycles",
            arch ->
                slices()
                    .assignedFrom(
                        moduleLayerSlices(
                            arch,
                            root ->
                                root
                                    + "."
                                    + layout.adapterSubpackage()
                                    + "."
                                    + layout.incomingSubpackage()))
                    .should()
                    .beFreeOfCycles()
                    .allowEmptyShould(true))
        .selecting(
            "One slice per module root, holding the classes in <module>.adapter.incoming.. of"
                + " that module; everything else is ignored.")
        .checking(
            "The slices form no dependency cycle between modules' incoming adapters. Slices are"
                + " per module root, so a cycle inside one module's incoming adapters is not"
                + " detected here, and dependencies into other layers do not count. DCA-CYC-005"
                + " covers the application layer per operation; no rule slices the adapters within"
                + " a module.");
  }

  /**
   * The immediate child packages of a module's application package, {@code shared} excepted, must
   * be free of cycles. In a grouped layout those children are features, in a flat layout they are
   * the use cases themselves.
   */
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
                + " reported here as well.",
            "break the cycle by moving the shared concept into one slice or behind a port");
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
        if (index == segments.length || segments[index].equals(layout.sharedSubpackage()))
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
