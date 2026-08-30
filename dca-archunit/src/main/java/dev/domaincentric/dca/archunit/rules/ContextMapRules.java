package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
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
import java.util.Locale;
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

  private static final String API = "api";
  private static final String EVENTS = "events";

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

  /** DCA-MAP-001. */
  public static DcaRule declarationsOnlyOnBoundedContexts() {
    return DcaRule.check(
        "DCA-MAP-001",
        "Upstream, ExternalUpstream, and Partnership may only be declared on bounded context"
            + " packages",
        "Context map declarations are reserved for bounded contexts — only a context can be"
            + " downstream of, or partner with, another",
        arch -> {
          for (String pkg : allRootPackages(arch)) {
            if (arch.packageAnnotation(pkg, BoundedContext.class).isPresent()) {
              continue;
            }
            requireNoDeclaration(arch, pkg, Upstream.class, "@Upstream");
            requireNoDeclaration(arch, pkg, ExternalUpstream.class, "@ExternalUpstream");
            requireNoDeclaration(arch, pkg, Partnership.class, "@Partnership");
          }
        });
  }

  private static void requireNoDeclaration(
      DcaArchitecture arch, String pkg, Class<? extends Annotation> type, String label) {
    require(
        arch.packageAnnotations(pkg, type).isEmpty(),
        "Package '"
            + pkg
            + "' declares "
            + label
            + " but is not a @BoundedContext — context map declarations are reserved for bounded"
            + " contexts");
  }

  /** DCA-MAP-002. */
  public static DcaRule externalUpstreamsWellFormed() {
    return DcaRule.check(
        "DCA-MAP-002",
        "ExternalUpstream declarations must be well-formed and unique per name and interaction",
        "The identity of an @ExternalUpstream declaration is (name, interaction); internal"
            + " contexts are declared with @Upstream instead",
        arch -> {
          Set<String> moduleNames = moduleNames(arch);
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
            List<String> edges = new ArrayList<>();
            for (ExternalUpstream e : arch.packageAnnotations(pkg, ExternalUpstream.class)) {
              require(
                  !e.name().isBlank(),
                  "Context '" + source + "' declares an @ExternalUpstream with a blank name");
              require(
                  !moduleNames.contains(e.name()),
                  "Context '"
                      + source
                      + "' declares external system '"
                      + e.name()
                      + "', which is an internal bounded context module — use @Upstream for"
                      + " internal contexts");
              String edge = e.name() + " :: " + e.interaction();
              require(
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
        });
  }

  /** DCA-MAP-003. */
  public static DcaRule externalSystemNamesDistinctAfterNormalization() {
    return DcaRule.check(
        "DCA-MAP-003",
        "Distinct external system names must not collide after mermaid id normalization",
        "The generated context map renders one node per normalized external system name — two"
            + " spellings of the same system would silently merge into one node",
        arch -> {
          Map<String, String> idToName = new LinkedHashMap<>();
          for (String pkg : arch.boundedContextPackages()) {
            for (ExternalUpstream e : arch.packageAnnotations(pkg, ExternalUpstream.class)) {
              String id = normalizedExternalId(e.name());
              String known = idToName.getOrDefault(id, e.name());
              require(
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
        });
  }

  /** DCA-MAP-004. */
  public static DcaRule upstreamsReferenceExistingContexts() {
    return DcaRule.check(
        "DCA-MAP-004",
        "Upstream declarations must reference an existing bounded context and never the declaring"
            + " context itself",
        "A dangling or self-referencing upstream edge describes a relationship that cannot exist",
        arch -> {
          Set<String> moduleNames = moduleNames(arch);
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
            for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
              require(
                  moduleNames.contains(u.context()),
                  "Context '"
                      + source
                      + "' declares @Upstream(context = \""
                      + u.context()
                      + "\") but no bounded context module with that name exists (known: "
                      + moduleNames
                      + ")");
              require(
                  !u.context().equals(source),
                  "Context '" + source + "' declares itself as its own upstream");
            }
          }
        });
  }

  /** DCA-MAP-005. */
  public static DcaRule upstreamsUniquePerContextAndChannel() {
    return DcaRule.check(
        "DCA-MAP-005",
        "Upstream declarations must be unique per context and channel, and via must not be empty",
        "The identity of an @Upstream declaration is (context, via); different translations per"
            + " channel require separate annotations",
        arch -> {
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
            List<String> edges = new ArrayList<>();
            for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
              require(
                  u.via().length > 0,
                  "Context '"
                      + source
                      + "': @Upstream(context = \""
                      + u.context()
                      + "\") declares no channel — via must not be empty");
              for (Upstream.Consumes channel : u.via()) {
                String edge = u.context() + " :: " + channelName(channel);
                require(
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
        });
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
          Optional<Class<? extends Annotation>> moduleAnnotation = moduleAnnotationType();
          if (moduleAnnotation.isEmpty()) {
            return;
          }
          Set<String> moduleNames = moduleNames(arch);
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
            Set<String> declared = declaredEdges(arch, pkg);
            Set<String> allowed = new LinkedHashSet<>();
            for (String entry : allowedDependencies(arch, pkg, moduleAnnotation.get())) {
              String normalized = entry.replaceAll("\\s*::\\s*", " :: ").trim();
              if (normalized.contains(" :: ")
                  && moduleNames.contains(normalized.split(" :: ")[0])) {
                allowed.add(normalized);
              }
            }
            require(
                declared.equals(allowed),
                "Context '"
                    + source
                    + "': @Upstream declarations "
                    + new TreeSet<>(declared)
                    + " and @ApplicationModule.allowedDependencies named-interface entries "
                    + new TreeSet<>(allowed)
                    + " must describe the same edges — neither side may know more than the other");
          }
        });
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
          Map<String, String> packagesByName = packagesByName(arch);
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
            for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
              String targetPkg = packagesByName.get(u.context());
              if (u.status() != Upstream.Status.IMPLEMENTED || targetPkg == null) {
                continue;
              }
              for (Upstream.Consumes channel : u.via()) {
                String channelPkg = targetPkg + "." + channelName(channel);
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
                require(
                    exists,
                    "Context '"
                        + source
                        + "' declares @Upstream(context = \""
                        + u.context()
                        + "\", via = "
                        + channelName(channel)
                        + ") as IMPLEMENTED, but no class in '"
                        + pkg
                        + "' depends on '"
                        + channelPkg
                        + "..' — implement the dependency, mark the declaration status = PLANNED,"
                        + " or remove it");
              }
            }
          }
        });
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
          Map<String, String> packagesByName = packagesByName(arch);
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
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
                noClasses()
                    .that()
                    .resideInAPackage(pkg + "..")
                    .and()
                    .resideOutsideOfPackage(allowedAdapter)
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(targetPkg + "." + channelName(channel) + "..")
                    .allowEmptyShould(true)
                    .because(
                        "Context '"
                            + source
                            + "' declares ANTI_CORRUPTION_LAYER towards '"
                            + u.context()
                            + "' ("
                            + channelName(channel)
                            + ") — upstream contract types must not leave "
                            + allowedAdapter
                            + "; translate them there into the context's own model")
                    .check(arch.classes());
              }
            }
          }
        });
  }

  /** DCA-MAP-009. */
  public DcaRule conformistNeverReachesDomain() {
    return DcaRule.check(
        "DCA-MAP-009",
        "Conformist: upstream contract types must never reach the domain layer",
        "Conformism does not suspend domain purity — the domain layer stays free of foreign"
            + " contract types",
        arch -> {
          Map<String, String> packagesByName = packagesByName(arch);
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
            for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
              String targetPkg = packagesByName.get(u.context());
              if (u.translation() != Upstream.Translation.CONFORMIST || targetPkg == null) {
                continue;
              }
              for (Upstream.Consumes channel : u.via()) {
                noClasses()
                    .that()
                    .resideInAPackage(layout.domainPattern(pkg))
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(targetPkg + "." + channelName(channel) + "..")
                    .allowEmptyShould(true)
                    .because(
                        "Context '"
                            + source
                            + "' conforms to '"
                            + u.context()
                            + "' ("
                            + channelName(channel)
                            + "), but conformism does not suspend domain purity — the domain"
                            + " layer stays free of foreign contract types")
                    .check(arch.classes());
              }
            }
          }
        });
  }

  /** DCA-MAP-010. */
  public DcaRule externalContractTypesRespectTranslation() {
    return DcaRule.check(
        "DCA-MAP-010",
        "External system contract types must respect the declared translation and interaction",
        "An external system's contract types are confined to the adapter where the exchange"
            + " crosses the boundary (ACL) or at least kept out of the domain (Conformist)",
        arch -> {
          // Without contractPackages (wire-level contract, no vendor SDK) there is nothing to
          // check — the declaration then only documents the relationship.
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
            for (ExternalUpstream e : arch.packageAnnotations(pkg, ExternalUpstream.class)) {
              if (e.contractPackages().length == 0) {
                continue;
              }
              if (e.translation() == Upstream.Translation.ANTI_CORRUPTION_LAYER) {
                String allowedAdapter =
                    e.interaction() == ExternalUpstream.Interaction.OUTBOUND
                        ? layout.outgoingAdapterPattern(pkg)
                        : layout.incomingAdapterPattern(pkg);
                noClasses()
                    .that()
                    .resideInAPackage(pkg + "..")
                    .and()
                    .resideOutsideOfPackage(allowedAdapter)
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(e.contractPackages())
                    .allowEmptyShould(true)
                    .because(
                        "Context '"
                            + source
                            + "' declares ANTI_CORRUPTION_LAYER towards external system '"
                            + e.name()
                            + "' ("
                            + e.interaction()
                            + ") — its contract types ("
                            + String.join(", ", e.contractPackages())
                            + ") must not leave "
                            + allowedAdapter)
                    .check(arch.classes());
              } else {
                noClasses()
                    .that()
                    .resideInAPackage(layout.domainPattern(pkg))
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(e.contractPackages())
                    .allowEmptyShould(true)
                    .because(
                        "Context '"
                            + source
                            + "' conforms to external system '"
                            + e.name()
                            + "', but conformism does not suspend domain purity — the domain"
                            + " layer stays free of its contract types")
                    .check(arch.classes());
              }
            }
          }
        });
  }

  /** DCA-MAP-011. */
  public static DcaRule crossContextDependenciesRequireDeclaration() {
    return DcaRule.check(
        "DCA-MAP-011",
        "Cross-context dependencies on published interfaces require an Upstream declaration",
        "Every real dependency on a foreign api/ or events/ package is a context-map edge and must"
            + " be declared as such",
        arch -> {
          List<String> contexts = arch.boundedContextPackages();
          for (String srcPkg : contexts) {
            String source = shortName(srcPkg);
            Set<String> declared = declaredEdges(arch, srcPkg);
            for (String tgtPkg : contexts) {
              if (tgtPkg.equals(srcPkg)) {
                continue;
              }
              String target = shortName(tgtPkg);
              for (String channel : List.of(API, EVENTS)) {
                if (declared.contains(target + " :: " + channel)) {
                  continue;
                }
                noClasses()
                    .that()
                    .resideInAPackage(srcPkg + "..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage(tgtPkg + "." + channel + "..")
                    .allowEmptyShould(true)
                    .because(
                        "Context '"
                            + source
                            + "' depends on '"
                            + target
                            + " :: "
                            + channel
                            + "' without declaring it — add @Upstream(context = \""
                            + target
                            + "\", translation = ..., via = ...) to its package-info")
                    .check(arch.classes());
              }
            }
          }
        });
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
          Map<String, String> packagesByName = packagesByName(arch);
          for (String pkg : arch.boundedContextPackages()) {
            String source = shortName(pkg);
            for (Partnership p : arch.packageAnnotations(pkg, Partnership.class)) {
              require(
                  packagesByName.containsKey(p.context()),
                  "Context '"
                      + source
                      + "' declares @Partnership(context = \""
                      + p.context()
                      + "\") but no bounded context module with that name exists");
              require(
                  !p.context().equals(source),
                  "Context '" + source + "' declares a partnership with itself");
              boolean reverse =
                  arch
                      .packageAnnotations(packagesByName.get(p.context()), Partnership.class)
                      .stream()
                      .anyMatch(r -> r.context().equals(source));
              require(
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
        });
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
            String source = shortName(pkg);
            for (Upstream u : arch.packageAnnotations(pkg, Upstream.class)) {
              for (Upstream.Consumes channel : u.via()) {
                System.out.println(
                    "  "
                        + source
                        + " --["
                        + u.translation()
                        + " / "
                        + channelName(channel)
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
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  private static void require(boolean condition, String message) {
    if (!condition) {
      throw new DcaRuleViolation("", List.of(message));
    }
  }

  private static Set<String> allRootPackages(DcaArchitecture arch) {
    Set<String> roots = new LinkedHashSet<>();
    for (JavaClass javaClass : arch.classes()) {
      String root = arch.rootContextPackage(javaClass.getPackageName());
      if (root != null) {
        roots.add(root);
      }
    }
    return roots;
  }

  private static Set<String> moduleNames(DcaArchitecture arch) {
    Set<String> names = new LinkedHashSet<>();
    arch.boundedContextPackages().forEach(p -> names.add(shortName(p)));
    return names;
  }

  private static Map<String, String> packagesByName(DcaArchitecture arch) {
    Map<String, String> byName = new LinkedHashMap<>();
    arch.boundedContextPackages().forEach(p -> byName.put(shortName(p), p));
    return byName;
  }

  /** All declared upstream edges of a context as "target :: channel" strings. */
  private static Set<String> declaredEdges(DcaArchitecture arch, String contextPackage) {
    Set<String> edges = new LinkedHashSet<>();
    for (Upstream u : arch.packageAnnotations(contextPackage, Upstream.class)) {
      for (Upstream.Consumes channel : u.via()) {
        edges.add(u.context() + " :: " + channelName(channel));
      }
    }
    return edges;
  }

  private static String channelName(Upstream.Consumes channel) {
    return channel == Upstream.Consumes.API ? API : EVENTS;
  }

  /** True when packageName is root itself or a sub-package of root (exact segment boundary). */
  private static boolean inPackageTree(String packageName, String root) {
    return packageName.equals(root) || packageName.startsWith(root + ".");
  }

  /** The mermaid node id of an external system in the generated context map. */
  private static String normalizedExternalId(String name) {
    return "ext_" + name.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "_");
  }

  private static String shortName(String packagePath) {
    return DcaArchitecture.simpleContextName(packagePath);
  }
}
