package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.contextmap.ContextMapRenderer;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.ExternalUpstream;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Partnership;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.Upstream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

/**
 * Rules for the executable Context Map.
 *
 * <p>The context map is declared as package annotations, each side declaring only what it controls:
 * {@code @Upstream} / {@code @ExternalUpstream} on the downstream side, {@code @Partnership} on
 * both sides, {@code @OpenHostService} on the upstream side, and — optionally — the module system's
 * {@code allowedDependencies} as the enforced package boundary. These rules prove the declarations
 * consistent with each other and with the actual code. Organizational patterns (Customer–Supplier
 * etc.) are deliberately not machine-classified; Separate Ways is the absence of any declaration.
 *
 * <p>The module-system agreement rule is the only framework-specific rule; it reads the module
 * annotation named by {@link
 * dev.domaincentric.dca.archunit.FrameworkAnnotations#applicationModule()} reflectively and is
 * skipped when that annotation is not configured or not on the class path.
 */
public final class ContextMapRules implements DcaRuleSet {

  private final DcaLayout layout;
  private final List<DcaRule> rules;

  public ContextMapRules(DcaLayout layout) {
    this.layout = layout;
    this.rules =
        List.of(
            declarationsOnlyOnBoundedContexts(),
            externalUpstreamsWellFormed(),
            externalSystemNamesDistinctAfterNormalization(),
            upstreamsReferenceExistingContexts(),
            upstreamsUniquePerContextAndChannel(),
            upstreamsAgreeWithModuleDependencies(),
            implementedUpstreamsBackedByCode(),
            antiCorruptionLayerStaysInAdapter(),
            conformistNeverReachesDomain(),
            externalContractTypesRespectTranslation(),
            crossContextDependenciesRequireDeclaration(),
            partnershipsSymmetric(),
            displayDeclaredContextMap());
  }

  @Override
  public String name() {
    return "contextmap";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  // ---------------------------------------------------------------------------------------------
  // Declaration well-formedness
  // ---------------------------------------------------------------------------------------------

  /**
   * DCA-MAP-001. Looks at every package below the base package that an imported class lives in, its
   * ancestors included — not only at the resolved context roots — so a relationship declared on a
   * nested package (a use-case package, a feature, a grouping package) is reported instead of being
   * silently ignored by every other rule and by the renderer. The package that carries the
   * declaration must itself be the {@code @BoundedContext}.
   */
  public static DcaRule declarationsOnlyOnBoundedContexts() {
    return DcaRule.check(
            "DCA-MAP-001",
            "Upstream, ExternalUpstream, and Partnership may only be declared on bounded context"
                + " packages",
            "Context map declarations are reserved for bounded contexts — only a context can be"
                + " downstream of, or partner with, another",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              for (String pkg : arch.packagesBelowBase()) {
                if (arch.packageAnnotation(pkg, BoundedContext.class).isPresent()) {
                  continue;
                }
                requireNoDeclaration(violations, arch, pkg, Upstream.class, "@Upstream");
                requireNoDeclaration(
                    violations, arch, pkg, ExternalUpstream.class, "@ExternalUpstream");
                requireNoDeclaration(violations, arch, pkg, Partnership.class, "@Partnership");
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every package at or below the base package that an imported class lives in,"
                + " its ancestors included, whose package-info does not carry @BoundedContext."
                + " The resolved context roots themselves are skipped.")
        .checking(
            "The package declares no @Upstream, @ExternalUpstream or @Partnership. A"
                + " declaration on a nested package (a use-case, feature or grouping package)"
                + " is reported; whether a declaration is well-formed is left to the other"
                + " rules.");
  }

  private static void requireNoDeclaration(
      CollectedViolations violations,
      DcaArchitecture arch,
      String pkg,
      Class<? extends Annotation> type,
      String label) {
    violations.require(
        arch.packageAnnotations(pkg, type).isEmpty(),
        "Package '"
            + pkg
            + "' declares "
            + label
            + " but is not a @BoundedContext — context map declarations are reserved for bounded"
            + " contexts; declare the relationship on the context's root package");
  }

  /** DCA-MAP-002. */
  public static DcaRule externalUpstreamsWellFormed() {
    return DcaRule.check(
            "DCA-MAP-002",
            "ExternalUpstream declarations must be well-formed and unique per name and interaction",
            "The identity of an @ExternalUpstream declaration is (name, interaction); internal"
                + " contexts are declared with @Upstream instead",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              Set<String> moduleNames = moduleNames(arch);
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                List<String> edges = new ArrayList<>();
                for (ExternalUpstream e : arch.packageAnnotations(pkg, ExternalUpstream.class)) {
                  violations.require(
                      !e.name().isBlank(),
                      "Context '" + source + "' declares an @ExternalUpstream with a blank name");
                  violations.require(
                      !moduleNames.contains(e.name()),
                      "Context '"
                          + source
                          + "' declares external system '"
                          + e.name()
                          + "', which is an internal bounded context module — use @Upstream for"
                          + " internal contexts");
                  String edge = e.name() + " :: " + e.interaction();
                  violations.require(
                      !edges.contains(edge),
                      "Context '"
                          + source
                          + "' declares external system edge '"
                          + edge
                          + "' more than once — the identity of an @ExternalUpstream declaration is"
                          + " (name, interaction)");
                  edges.add(edge);
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @ExternalUpstream declaration on the package-info of every package"
                + " carrying @BoundedContext, reading name() and interaction(). PLANNED"
                + " declarations are included.")
        .checking(
            "name() is not blank and is not the name of an internal bounded context (a"
                + " context's package name relative to the base package), and the pair (name,"
                + " interaction) occurs at most once per declaring context. The same external"
                + " system declared by two different contexts is not reported; translation()"
                + " and contractPackages() are not checked here.");
  }

  /** DCA-MAP-003. */
  public static DcaRule externalSystemNamesDistinctAfterNormalization() {
    return DcaRule.check(
            "DCA-MAP-003",
            "Distinct external system names must not collide after mermaid id normalization",
            "The generated context map renders one node per normalized external system name — two"
                + " spellings of the same system would silently merge into one node",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              Map<String, String> idToName = new LinkedHashMap<>();
              for (String pkg : arch.boundedContextPackages()) {
                for (ExternalUpstream e : arch.packageAnnotations(pkg, ExternalUpstream.class)) {
                  String id = normalizedExternalId(e.name());
                  String known = idToName.getOrDefault(id, e.name());
                  violations.require(
                      known.equals(e.name()),
                      "External system names '"
                          + known
                          + "' and '"
                          + e.name()
                          + "' normalize to the same mermaid node id '"
                          + id
                          + "' — use one canonical spelling");
                  idToName.put(id, e.name());
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @ExternalUpstream declaration on the package-info of every package"
                + " carrying @BoundedContext, across all contexts, reading name(). PLANNED"
                + " declarations are included.")
        .checking(
            "Two declarations whose name() differs but normalizes to the same node id of"
                + " the generated context map (the renderer's own normalization) are reported"
                + " as a collision. Repeating one spelling of a name is not a collision.");
  }

  /** DCA-MAP-004. */
  public static DcaRule upstreamsReferenceExistingContexts() {
    return DcaRule.check(
            "DCA-MAP-004",
            "Upstream declarations must reference an existing bounded context and never the declaring"
                + " context itself",
            "A dangling or self-referencing upstream edge describes a relationship that cannot exist",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              Set<String> moduleNames = moduleNames(arch);
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
                  violations.require(
                      moduleNames.contains(u.context()),
                      "Context '"
                          + source
                          + "' declares @Upstream(context = \""
                          + u.context()
                          + "\") but no bounded context module with that name exists (known: "
                          + moduleNames
                          + ")");
                  violations.require(
                      !u.context().equals(source),
                      "Context '" + source + "' declares itself as its own upstream");
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @Upstream declaration on the package-info of every package carrying"
                + " @BoundedContext, reading context(). PLANNED declarations are included.")
        .checking(
            "context() names an existing bounded context - a package carrying"
                + " @BoundedContext, identified by its name relative to the base package - and"
                + " is not the declaring context itself. Whether any code depends on the target"
                + " is not established here.");
  }

  /** DCA-MAP-005. */
  public static DcaRule upstreamsUniquePerContextAndChannel() {
    return DcaRule.check(
            "DCA-MAP-005",
            "Upstream declarations must be unique per context and channel, and via must not be empty",
            "The identity of an @Upstream declaration is (context, via); different translations per"
                + " channel require separate annotations",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                List<String> edges = new ArrayList<>();
                for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
                  violations.require(
                      u.via().length > 0,
                      "Context '"
                          + source
                          + "': @Upstream(context = \""
                          + u.context()
                          + "\") declares no channel — via must not be empty");
                  for (Upstream.Consumes channel : u.via()) {
                    String edge = u.context() + " :: " + channelName(arch, channel);
                    violations.require(
                        !edges.contains(edge),
                        "Context '"
                            + source
                            + "' declares (context, channel) '"
                            + edge
                            + "' more than once — the identity of an @Upstream declaration is"
                            + " (context, via); different translations per channel require separate"
                            + " annotations");
                    edges.add(edge);
                  }
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @Upstream declaration on the package-info of every package carrying"
                + " @BoundedContext, reading context() and via(). PLANNED declarations are"
                + " included.")
        .checking(
            "via() holds at least one channel, and the pair (context, channel) - the"
                + " channel resolved to the layout's api or events sub-package name - occurs at"
                + " most once among the declarations of one context. Two declarations towards"
                + " the same context on different channels are allowed; the same context and"
                + " channel declared twice, even with different translations, is reported.");
  }

  // ---------------------------------------------------------------------------------------------
  // Consistency with the module system (skipped when no module annotation is available)
  // ---------------------------------------------------------------------------------------------

  /** DCA-MAP-006. */
  public DcaRule upstreamsAgreeWithModuleDependencies() {
    return DcaRule.check(
            "DCA-MAP-006",
            "Upstream declarations and Spring Modulith allowedDependencies must agree",
            "Neither the context map nor the module boundary may know more than the other — an edge"
                + " that exists only on one side is stale",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              Optional<Class<? extends Annotation>> moduleAnnotation = moduleAnnotationType();
              if (moduleAnnotation.isEmpty()) {
                return;
              }
              Set<String> moduleNames = moduleNames(arch);
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                Set<String> declared = declaredEdges(arch, pkg);
                Set<String> allowed = new LinkedHashSet<>();
                for (String entry : allowedDependencies(arch, pkg, moduleAnnotation.get())) {
                  String normalized = entry.replaceAll("\\s*::\\s*", " :: ").trim();
                  if (normalized.contains(" :: ")
                      && moduleNames.contains(normalized.split(" :: ")[0])) {
                    allowed.add(normalized);
                  }
                }
                violations.require(
                    declared.equals(allowed),
                    "Context '"
                        + source
                        + "': @Upstream declarations "
                        + new TreeSet<>(declared)
                        + " and @ApplicationModule.allowedDependencies named-interface entries "
                        + new TreeSet<>(allowed)
                        + " must describe the same edges — neither side may know more than the other");
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every package carrying @BoundedContext, provided the layout names a module"
                + " annotation (Spring Modulith's @ApplicationModule) that is on the class"
                + " path; reads its @Upstream declarations (context(), via(); PLANNED included)"
                + " and, reflectively, the module annotation's allowedDependencies attribute."
                + " Without a configured and loadable module annotation the rule selects"
                + " nothing and passes.")
        .checking(
            "The set of declared edges 'context :: channel' equals the set of"
                + " allowedDependencies entries of the form 'module :: named-interface' whose"
                + " module is a bounded context, whitespace around '::' normalized. Entries"
                + " without '::' and entries naming a non-context module are ignored; a context"
                + " whose package-info carries no module annotation contributes an empty set,"
                + " so its @Upstream declarations are reported as unmatched.");
  }

  @SuppressWarnings("unchecked")
  private Optional<Class<? extends Annotation>> moduleAnnotationType() {
    if (!layout.frameworkAnnotations().hasApplicationModule()) {
      return Optional.empty();
    }
    String name = layout.frameworkAnnotations().applicationModule();
    try {
      Class<?> type = Class.forName(name, false, Thread.currentThread().getContextClassLoader());
      return type.isAnnotation()
          ? Optional.of((Class<? extends Annotation>) type)
          : Optional.empty();
    } catch (ClassNotFoundException e) {
      return Optional.empty();
    }
  }

  private static List<String> allowedDependencies(
      DcaArchitecture arch, String pkg, Class<? extends Annotation> moduleAnnotation) {
    Optional<? extends Annotation> module = arch.packageAnnotation(pkg, moduleAnnotation);
    if (module.isEmpty()) {
      return List.of();
    }
    try {
      Method attribute = moduleAnnotation.getMethod("allowedDependencies");
      Object value = attribute.invoke(module.get());
      return value instanceof String[] ? Arrays.asList((String[]) value) : List.of();
    } catch (ReflectiveOperationException e) {
      return List.of();
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Consistency with the code
  // ---------------------------------------------------------------------------------------------

  /** DCA-MAP-007. */
  public static DcaRule implementedUpstreamsBackedByCode() {
    return DcaRule.check(
            "DCA-MAP-007",
            "Implemented Upstream declarations must be backed by an actual code dependency",
            "A declared IMPLEMENTED edge without any real dependency is stale (or premature — then it"
                + " is PLANNED) and would otherwise pass forever alongside an equally stale module"
                + " boundary entry",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              Map<String, String> packagesByName = packagesByName(arch);
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
                  String targetPkg = packagesByName.get(u.context());
                  if (u.status() != Upstream.Status.IMPLEMENTED || targetPkg == null) {
                    continue;
                  }
                  for (Upstream.Consumes channel : u.via()) {
                    String channelPkg = targetPkg + "." + channelName(arch, channel);
                    boolean exists = false;
                    for (JavaClass javaClass : arch.classes()) {
                      if (!inPackageTree(javaClass.getPackageName(), pkg)) {
                        continue;
                      }
                      for (Dependency dep : javaClass.getDirectDependenciesFromSelf()) {
                        if (inPackageTree(dep.getTargetClass().getPackageName(), channelPkg)) {
                          exists = true;
                          break;
                        }
                      }
                      if (exists) {
                        break;
                      }
                    }
                    violations.require(
                        exists,
                        "Context '"
                            + source
                            + "' declares @Upstream(context = \""
                            + u.context()
                            + "\", via = "
                            + channelName(arch, channel)
                            + ") as IMPLEMENTED, but no class in '"
                            + pkg
                            + "' depends on '"
                            + channelPkg
                            + "..' — implement the dependency, mark the declaration status = PLANNED,"
                            + " or remove it");
                  }
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @Upstream declaration with status() IMPLEMENTED on the package-info"
                + " of every package carrying @BoundedContext whose context() names an existing"
                + " bounded context, reading via(). PLANNED declarations and declarations"
                + " towards an unknown context are skipped.")
        .checking(
            "For every channel in via(), at least one class anywhere below the declaring"
                + " context's package has a direct dependency on a class in the target"
                + " context's channel sub-package (api or events per the layout) or below."
                + " Which layer holds the dependency is not checked here.");
  }

  // ---------------------------------------------------------------------------------------------
  // Translation enforcement (channel-dependent)
  // ---------------------------------------------------------------------------------------------

  /** DCA-MAP-008. */
  public DcaRule antiCorruptionLayerStaysInAdapter() {
    return DcaRule.check(
            "DCA-MAP-008",
            "Anti-Corruption Layer: upstream contract types must stay inside the matching adapter",
            "The ACL sits where the dependency crosses the boundary — outgoing adapters for synchronous"
                + " API calls, incoming adapters for consumed events — and translates the upstream"
                + " contract into the context's own model there",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              Map<String, String> packagesByName = packagesByName(arch);
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
                  String targetPkg = packagesByName.get(u.context());
                  if (u.translation() != Upstream.Translation.ANTI_CORRUPTION_LAYER
                      || targetPkg == null) {
                    continue;
                  }
                  for (Upstream.Consumes channel : u.via()) {
                    String allowedAdapter =
                        channel == Upstream.Consumes.API
                            ? layout.outgoingAdapterPattern(pkg)
                            : layout.incomingAdapterPattern(pkg);
                    violations.addAll(
                        noClasses()
                            .that()
                            .resideInAPackage(pkg + "..")
                            .and()
                            .resideOutsideOfPackage(allowedAdapter)
                            .should()
                            .dependOnClassesThat()
                            .resideInAPackage(targetPkg + "." + channelName(arch, channel) + "..")
                            .allowEmptyShould(true),
                        arch.classes(),
                        "Context '"
                            + source
                            + "' declares ANTI_CORRUPTION_LAYER towards '"
                            + u.context()
                            + "' ("
                            + channelName(arch, channel)
                            + ") — upstream contract types must not leave "
                            + allowedAdapter
                            + "; translate them there into the context's own model");
                  }
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @Upstream declaration with translation() ANTI_CORRUPTION_LAYER on the"
                + " package-info of every package carrying @BoundedContext whose context()"
                + " names an existing bounded context, reading via(). status() is not"
                + " consulted, so PLANNED declarations are checked too; declarations towards an"
                + " unknown context are skipped.")
        .checking(
            "No class below the declaring context's package outside the matching adapter"
                + " depends on a class in the target context's channel sub-package or below:"
                + " the outgoing adapter (<context>.adapter.outgoing..) for the API channel,"
                + " the incoming adapter (<context>.adapter.incoming..) for the EVENTS channel."
                + " That the adapter actually translates the contract into the context's own"
                + " model is not established.");
  }

  /** DCA-MAP-009. */
  public DcaRule conformistNeverReachesDomain() {
    return DcaRule.check(
            "DCA-MAP-009",
            "Conformist: upstream contract types must never reach the domain layer",
            "Conformism does not suspend domain purity — the domain layer stays free of foreign"
                + " contract types",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              Map<String, String> packagesByName = packagesByName(arch);
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
                  String targetPkg = packagesByName.get(u.context());
                  if (u.translation() != Upstream.Translation.CONFORMIST || targetPkg == null) {
                    continue;
                  }
                  for (Upstream.Consumes channel : u.via()) {
                    violations.addAll(
                        noClasses()
                            .that()
                            .resideInAPackage(layout.domainPattern(pkg))
                            .should()
                            .dependOnClassesThat()
                            .resideInAPackage(targetPkg + "." + channelName(arch, channel) + "..")
                            .allowEmptyShould(true),
                        arch.classes(),
                        "Context '"
                            + source
                            + "' conforms to '"
                            + u.context()
                            + "' ("
                            + channelName(arch, channel)
                            + "), but conformism does not suspend domain purity — the domain"
                            + " layer stays free of foreign contract types");
                  }
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @Upstream declaration with translation() CONFORMIST on the"
                + " package-info of every package carrying @BoundedContext whose context()"
                + " names an existing bounded context, reading via(). status() is not"
                + " consulted, so PLANNED declarations are checked too; declarations towards an"
                + " unknown context are skipped.")
        .checking(
            "No class in the declaring context's domain layer (<context>.domain..)"
                + " depends on a class in the target context's channel sub-package (api or"
                + " events per the layout) or below. Application and adapter classes may use"
                + " the upstream's contract types.");
  }

  /** DCA-MAP-010. */
  public DcaRule externalContractTypesRespectTranslation() {
    return DcaRule.check(
            "DCA-MAP-010",
            "External system contract types must respect the declared translation and interaction",
            "An external system's contract types are confined to the adapter where the exchange"
                + " crosses the boundary (ACL) or at least kept out of the domain (Conformist)",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              // Without contractPackages (wire-level contract, no vendor SDK) there is nothing to
              // check — the declaration then only documents the relationship.
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                for (ExternalUpstream e : arch.packageAnnotations(pkg, ExternalUpstream.class)) {
                  if (e.contractPackages().length == 0) {
                    continue;
                  }
                  if (e.translation() == Upstream.Translation.ANTI_CORRUPTION_LAYER) {
                    String allowedAdapter =
                        e.interaction() == ExternalUpstream.Interaction.OUTBOUND
                            ? layout.outgoingAdapterPattern(pkg)
                            : layout.incomingAdapterPattern(pkg);
                    violations.addAll(
                        noClasses()
                            .that()
                            .resideInAPackage(pkg + "..")
                            .and()
                            .resideOutsideOfPackage(allowedAdapter)
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage(e.contractPackages())
                            .allowEmptyShould(true),
                        arch.classes(),
                        "Context '"
                            + source
                            + "' declares ANTI_CORRUPTION_LAYER towards external system '"
                            + e.name()
                            + "' ("
                            + e.interaction()
                            + ") — its contract types ("
                            + String.join(", ", e.contractPackages())
                            + ") must not leave "
                            + allowedAdapter);
                  } else {
                    violations.addAll(
                        noClasses()
                            .that()
                            .resideInAPackage(layout.domainPattern(pkg))
                            .should()
                            .dependOnClassesThat()
                            .resideInAnyPackage(e.contractPackages())
                            .allowEmptyShould(true),
                        arch.classes(),
                        "Context '"
                            + source
                            + "' conforms to external system '"
                            + e.name()
                            + "', but conformism does not suspend domain purity — the domain"
                            + " layer stays free of its contract types");
                  }
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @ExternalUpstream declaration on the package-info of every package"
                + " carrying @BoundedContext whose contractPackages() is not empty, reading"
                + " translation() and interaction(). status() is not consulted. A declaration"
                + " without contractPackages() (wire-level contract, no vendor SDK) is skipped"
                + " - it only documents the relationship.")
        .checking(
            "With ANTI_CORRUPTION_LAYER, no class below the declaring context's package"
                + " outside the matching adapter - <context>.adapter.outgoing.. for OUTBOUND,"
                + " <context>.adapter.incoming.. for INBOUND - depends on a class in any of the"
                + " contract packages. With any other translation (CONFORMIST), no class in"
                + " <context>.domain.. does. That the adapter actually translates the contract"
                + " is not established.");
  }

  /** DCA-MAP-011. */
  public static DcaRule crossContextDependenciesRequireDeclaration() {
    return DcaRule.check(
            "DCA-MAP-011",
            "Cross-context dependencies on published interfaces require an Upstream declaration",
            "Every real dependency on a foreign api/ or events/ package is a context-map edge and must"
                + " be declared as such",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              List<String> contexts = arch.boundedContextPackages();
              for (String srcPkg : contexts) {
                String source = arch.contextName(srcPkg);
                Set<String> declared = declaredEdges(arch, srcPkg);
                for (String tgtPkg : contexts) {
                  if (tgtPkg.equals(srcPkg)) {
                    continue;
                  }
                  String target = arch.contextName(tgtPkg);
                  for (String channel : arch.layout().publishedSubpackages()) {
                    if (declared.contains(target + " :: " + channel)) {
                      continue;
                    }
                    violations.addAll(
                        noClasses()
                            .that()
                            .resideInAPackage(srcPkg + "..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAPackage(tgtPkg + "." + channel + "..")
                            .allowEmptyShould(true),
                        arch.classes(),
                        "Context '"
                            + source
                            + "' depends on '"
                            + target
                            + " :: "
                            + channel
                            + "' without declaring it — add @Upstream(context = \""
                            + target
                            + "\", translation = ..., via = ...) to its package-info");
                  }
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every ordered pair of two distinct packages carrying @BoundedContext,"
                + " combined with each published sub-package of the layout (api, events), for"
                + " which the source context declares no @Upstream with context() naming the"
                + " target and via() containing that channel. PLANNED declarations count as"
                + " declared.")
        .checking(
            "No class below the source context's package depends on a class in the"
                + " target context's channel sub-package or below. Dependencies on a foreign"
                + " context's other packages (domain, application, adapter) are not reported by"
                + " this rule.");
  }

  // ---------------------------------------------------------------------------------------------
  // Partnership symmetry
  // ---------------------------------------------------------------------------------------------

  /** DCA-MAP-012. */
  public static DcaRule partnershipsSymmetric() {
    return DcaRule.check(
            "DCA-MAP-012",
            "Partnership declarations must reference an existing bounded context, never themselves,"
                + " and must be symmetric",
            "A partnership is a mutual commitment — it exists only when both contexts declare it",
            arch -> {
              CollectedViolations violations = CollectedViolations.withoutHeader();
              Map<String, String> packagesByName = packagesByName(arch);
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                for (Partnership p : arch.packageAnnotations(pkg, Partnership.class)) {
                  if (!packagesByName.containsKey(p.context())) {
                    violations.add(
                        "Context '"
                            + source
                            + "' declares @Partnership(context = \""
                            + p.context()
                            + "\") but no bounded context module with that name exists");
                    continue;
                  }
                  if (p.context().equals(source)) {
                    violations.add("Context '" + source + "' declares a partnership with itself");
                    continue;
                  }
                  boolean reverse =
                      arch
                          .packageAnnotations(packagesByName.get(p.context()), Partnership.class)
                          .stream()
                          .anyMatch(r -> r.context().equals(source));
                  violations.require(
                      reverse,
                      "Partnership between '"
                          + source
                          + "' and '"
                          + p.context()
                          + "' is only declared on '"
                          + source
                          + "' — partnerships are symmetric, add @Partnership(context = \""
                          + source
                          + "\") to '"
                          + p.context()
                          + "'");
                }
              }
              violations.throwIfAny();
            })
        .selecting(
            "Every @Partnership declaration on the package-info of every package"
                + " carrying @BoundedContext, reading context().")
        .checking(
            "context() names an existing bounded context and is not the declaring"
                + " context itself, and the target context's package-info carries a"
                + " @Partnership whose context() names the declaring context in turn. A"
                + " partnership grants no dependency permission - whether any code dependency"
                + " exists between the two contexts is not checked.");
  }

  // ---------------------------------------------------------------------------------------------
  // Diagnostic
  // ---------------------------------------------------------------------------------------------

  /** DCA-MAP-013 — never fails. */
  public static DcaRule displayDeclaredContextMap() {
    return DcaRule.check(
            "DCA-MAP-013",
            "Diagnostic: Display declared context map",
            "Printing the declared edges makes the executable context map reviewable at a glance",
            arch -> {
              System.out.println("=== Context Map (declared) ===");
              for (String pkg : arch.boundedContextPackages()) {
                String source = arch.contextName(pkg);
                for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
                  for (Upstream.Consumes channel : u.via()) {
                    System.out.println(
                        "  "
                            + source
                            + " --["
                            + u.translation()
                            + " / "
                            + channelName(arch, channel)
                            + "]--> "
                            + u.context());
                  }
                }
                for (ExternalUpstream e : arch.packageAnnotations(pkg, ExternalUpstream.class)) {
                  System.out.println(
                      "  "
                          + source
                          + " --["
                          + e.translation()
                          + " / "
                          + e.interaction()
                          + "]--> (external) "
                          + e.name());
                }
                for (Partnership p : arch.packageAnnotations(pkg, Partnership.class)) {
                  System.out.println("  " + source + " <--[PARTNERSHIP]--> " + p.context());
                }
              }
              System.out.println("==============================");
            })
        .selecting(
            "Every @Upstream, @ExternalUpstream and @Partnership declaration on the"
                + " package-info of every package carrying @BoundedContext, reading context()"
                + " or name(), translation(), and via() or interaction().")
        .checking(
            "Informational - prints every declared edge to standard output and never"
                + " fails; it carries no assertion. status() is not printed, so a PLANNED edge"
                + " is listed like an implemented one.");
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  private static Set<String> moduleNames(DcaArchitecture arch) {
    Set<String> names = new LinkedHashSet<>();
    arch.boundedContextPackages().forEach(p -> names.add(arch.contextName(p)));
    return names;
  }

  private static Map<String, String> packagesByName(DcaArchitecture arch) {
    Map<String, String> byName = new LinkedHashMap<>();
    arch.boundedContextPackages().forEach(p -> byName.put(arch.contextName(p), p));
    return byName;
  }

  /** All declared upstream edges of a context as "target :: channel" strings. */
  private static Set<String> declaredEdges(DcaArchitecture arch, String contextPackage) {
    Set<String> edges = new LinkedHashSet<>();
    for (Upstream u : arch.packageAnnotations(contextPackage, Upstream.class)) {
      for (Upstream.Consumes channel : u.via()) {
        edges.add(u.context() + " :: " + channelName(arch, channel));
      }
    }
    return edges;
  }

  private static String channelName(DcaArchitecture arch, Upstream.Consumes channel) {
    return arch.layout().channelSubpackage(channel);
  }

  private static boolean inPackageTree(String packageName, String root) {
    return DcaArchitecture.inPackageTree(packageName, root);
  }

  /** The mermaid node id of an external system — the renderer's own normalisation. */
  private static String normalizedExternalId(String name) {
    return ContextMapRenderer.externalSystemNodeId(name);
  }
}
