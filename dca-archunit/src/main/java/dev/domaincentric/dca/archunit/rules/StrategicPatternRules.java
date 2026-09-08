package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.lang.ArchRule;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.OpenHostService;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * DDD strategic pattern rules: shared-kernel independence, bounded-context isolation, Open Host
 * Services, Integration Events and Anti-Corruption Layers. The isolation rules ({@code
 * DCA-STR-003}, {@code -004}, {@code -006}) select structurally over every module that owns a DCA
 * layer ({@link DcaArchitecture#isolatedModuleRoots()}), so a module governs and is protected
 * whether or not it declares {@code @BoundedContext}; the declaration decides context-map
 * membership. The shared kernel is discovered via {@code @SharedKernel} on {@code package-info}.
 */
public final class StrategicPatternRules implements DcaRuleSet {

  private final DcaLayout layout;
  private final List<DcaRule> rules;

  public StrategicPatternRules(DcaLayout layout) {
    this.layout = layout;
    this.rules =
        List.of(
            displayDiscoveredBoundedContexts(),
            sharedKernelMustNotDependOnBoundedContexts(),
            applicationLayerIsolation(),
            domainLayerIsolation(),
            openHostServicesResideInApiOrIncomingAdapter(),
            outgoingAdaptersOnlyUseOpenHostServices(),
            integrationEventsResideInEventsPackages(),
            integrationEventsAreRecords(),
            antiCorruptionLayerComponentsResideInAclPackages(),
            eventListenersUseAntiCorruptionLayer());
  }

  @Override
  public String name() {
    return "strategic";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  /** DCA-STR-001. */
  public static DcaRule displayDiscoveredBoundedContexts() {
    return DcaRule.check(
            "DCA-STR-001",
            "Diagnostic: Display discovered bounded contexts",
            "Making the discovered contexts visible shows which packages the strategic rules govern",
            arch -> {
              System.out.println("=== Discovered Bounded Contexts ===");
              for (Map.Entry<String, BoundedContext> e : arch.boundedContexts().entrySet()) {
                System.out.println("  " + e.getValue().name() + ": " + e.getKey());
                if (!e.getValue().description().isEmpty()) {
                  System.out.println("    Description: " + e.getValue().description());
                }
              }
              System.out.println("=== Shared Kernel ===");
              System.out.println("  Package: " + arch.sharedKernelPackage().orElse("<none>"));
              System.out.println("==================================");
            })
        .selecting(
            "Every package whose package-info carries @BoundedContext, at any depth below the base"
                + " package, plus the package annotated with @SharedKernel if there is one. Modules"
                + " that own layers without declaring @BoundedContext are not listed.")
        .checking(
            "Diagnostic - prints each discovered context's name, package and description and the"
                + " shared kernel package to standard output. It asserts nothing and never fails.");
  }

  /** DCA-STR-002. */
  public static DcaRule sharedKernelMustNotDependOnBoundedContexts() {
    return DcaRule.check(
            "DCA-STR-002",
            "Shared Kernel must not have dependencies on any bounded context",
            "Shared Kernel must be context-independent — it is shared by all contexts and owned by"
                + " none",
            arch -> {
              Optional<String> sharedKernel = arch.sharedKernelPackage();
              if (sharedKernel.isEmpty()) {
                return;
              }
              CollectedViolations violations = CollectedViolations.withoutHeader();
              for (Map.Entry<String, BoundedContext> ctx : arch.boundedContexts().entrySet()) {
                violations.addAll(
                    noClasses()
                        .that()
                        .resideInAPackage(sharedKernel.get() + "..")
                        .should()
                        .dependOnClassesThat()
                        .resideInAPackage(ctx.getKey() + "..")
                        .allowEmptyShould(true),
                    arch.classes(),
                    "Shared Kernel must not depend on bounded context '"
                        + ctx.getValue().name()
                        + "' ("
                        + ctx.getKey()
                        + ") - Shared Kernel must be context-independent");
              }
              violations.throwIfAny();
            })
        .selecting(
            "Classes in the package annotated with @SharedKernel and all its sub-packages. When no"
                + " shared kernel is declared nothing is selected and the rule passes.")
        .checking(
            "No selected class depends on a class in a package carrying @BoundedContext or below"
                + " it, checked once per declared context and reported together. A module that owns"
                + " layers without declaring @BoundedContext is not a forbidden target here."
                + " Dependencies on the base package outside any context, on infrastructure and on"
                + " third-party code are not checked.");
  }

  /**
   * DCA-STR-003. Selects over every module that owns a DCA layer ({@link
   * DcaArchitecture#isolatedModuleRoots()}), declared as a bounded context or not — on the source
   * side and on the target side. A module must not be able to escape isolation, or to have its
   * internals reached into, by staying off the context map.
   */
  public DcaRule applicationLayerIsolation() {
    return DcaRule.check(
            "DCA-STR-003",
            "Modules must not access each other in the application layer",
            "An application layer talks to other modules through its own output ports, implemented by"
                + " adapters - never directly. Selects structurally over every module that owns a DCA"
                + " layer, declared as a bounded context or not: an undeclared module must not be able"
                + " to escape isolation by staying off the context map",
            arch -> {
              List<ArchRule> perModule = new ArrayList<>();
              for (String source : arch.isolatedModuleRoots()) {
                String[] forbidden = arch.moduleRootPatternsExcluding(source);
                if (forbidden.length == 0) {
                  continue;
                }
                // dependOnClassesThat, not accessClassesThat: "access" is a method call or field
                // access, so a field, parameter or record component of a foreign type slips past
                // it.
                perModule.add(
                    noClasses()
                        .that()
                        .resideInAPackage(layout.applicationPattern(source))
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(forbidden)
                        .allowEmptyShould(true)
                        .because(
                            "The application layer of module '"
                                + arch.contextName(source)
                                + "' must not access other modules directly - define output ports and"
                                + " use adapters instead"));
              }
              CollectedViolations.check(perModule, arch.classes());
            })
        .selecting(
            "Classes in <module>.application.. of every isolated module root - every package below"
                + " the base package that owns a domain, application or adapter package, declared"
                + " as a bounded context or not, the shared kernel excluded. A module that is the only"
                + " isolated root has no foreign target and is skipped.")
        .checking(
            "No selected class depends on any class in another isolated module root or below it"
                + " (<other>..), the other module's api and events packages included. A dependency"
                + " is any reference - field, parameter, return type, record component, type"
                + " argument or call - not only a method call. Dependencies on the shared kernel,"
                + " on infrastructure and on third-party code are not checked. Violations are"
                + " collected per module and reported together.");
  }

  /** DCA-STR-004. */
  public DcaRule domainLayerIsolation() {
    return DcaRule.check(
            "DCA-STR-004",
            "Modules must not access each other in the domain layer",
            "A domain layer talks to its own module and the shared kernel, nothing else - not even"
                + " another module's api/. Selects structurally over every module that owns a DCA layer,"
                + " declared as a bounded context or not",
            arch -> {
              List<ArchRule> perModule = new ArrayList<>();
              for (String source : arch.isolatedModuleRoots()) {
                String[] forbidden = arch.moduleRootPatternsExcluding(source);
                if (forbidden.length == 0) {
                  continue;
                }
                perModule.add(
                    noClasses()
                        .that()
                        .resideInAPackage(layout.domainPattern(source))
                        .should()
                        .dependOnClassesThat()
                        .resideInAnyPackage(forbidden)
                        .allowEmptyShould(true)
                        .because(
                            "The domain layer of module '"
                                + arch.contextName(source)
                                + "' must depend on nothing outside its own module and the shared"
                                + " kernel"));
              }
              CollectedViolations.check(perModule, arch.classes());
            })
        .selecting(
            "Classes in <module>.domain.. of every isolated module root - every package below the"
                + " base package that owns a domain, application or adapter package, declared as a"
                + " bounded context or not, the shared kernel excluded. A module that is the only"
                + " isolated root has no foreign target and is skipped.")
        .checking(
            "No selected class depends on any class in another isolated module root or below it"
                + " (<other>..) - not even on its published api or events packages. Dependencies"
                + " on the shared kernel, on the module's own application, adapter and"
                + " infrastructure packages and on third-party code are not checked by this rule."
                + " Violations are collected per module and reported together.");
  }

  /**
   * DCA-STR-005. An Open Host Service is a relationship pattern, not a transport: the published
   * protocol one context offers to many consumers. In-process it is the {@code api} package; over
   * the network it is an incoming adapter (REST, gRPC, MCP, ...). Which sub-package of the incoming
   * adapter it sits in is the project's business.
   */
  public DcaRule openHostServicesResideInApiOrIncomingAdapter() {
    return DcaRule.of(
            "DCA-STR-005",
            "Open Host Services must be published: in the api package or as an incoming adapter",
            "An Open Host Service is the protocol a context publishes for other contexts - in-process"
                + " as its api/ package, over the network as an incoming adapter (REST, gRPC, MCP). It"
                + " belongs at the context boundary, never in the domain or application layer; the"
                + " adapter's sub-package is irrelevant",
            arch ->
                classes()
                    .that()
                    .areAnnotatedWith(OpenHostService.class)
                    .should()
                    .resideInAnyPackage(
                        ".." + layout.apiSubpackage() + "..",
                        ".."
                            + layout.adapterSubpackage()
                            + "."
                            + layout.incomingSubpackage()
                            + "..")
                    .allowEmptyShould(true))
        .selecting(
            "Classes annotated with @OpenHostService anywhere on the classpath under scan, in any"
                + " module or none.")
        .checking(
            "Each resides in a package whose path contains the configured api segment (..api..) or"
                + " the configured incoming-adapter segments (..adapter.incoming..), at any depth"
                + " and in any sub-package. One annotated in a domain, application or"
                + " outgoing-adapter package is reported. Which module publishes it, and whether"
                + " anyone consumes it, is not checked.");
  }

  /** DCA-STR-006. */
  /**
   * DCA-STR-006. The allow-list is the package convention {@code api} / {@code events} of the
   * target module — DCA's in-process contract, a convention of the architecture and not of any
   * framework. Everything else in a foreign module (its domain, application, adapters,
   * infrastructure) is internal.
   */
  public DcaRule outgoingAdaptersOnlyUseOpenHostServices() {
    return DcaRule.check(
            "DCA-STR-006",
            "Outgoing adapters accessing other modules must only use their published api/ and events/"
                + " packages",
            "Cross-module communication goes through the target's published api/ (synchronous) and"
                + " events/ (asynchronous) packages - DCA's in-process contract convention, package"
                + " names rather than framework annotations - never through its domain, application,"
                + " adapter or infrastructure packages. Selects structurally over every module that owns"
                + " a DCA layer, declared as a bounded context or not",
            arch -> {
              List<ArchRule> perModule = new ArrayList<>();
              for (String source : arch.isolatedModuleRoots()) {
                String[] foreign = arch.moduleRootPatternsExcluding(source);
                if (foreign.length == 0) {
                  continue;
                }
                String[] published = arch.publishedPackagePatternsExcluding(source);
                perModule.add(
                    noClasses()
                        .that()
                        .resideInAPackage(layout.outgoingAdapterPattern(source))
                        .should()
                        .dependOnClassesThat(
                            resideInAnyPackage(foreign).and(not(resideInAnyPackage(published))))
                        .allowEmptyShould(true)
                        .because(
                            "Outgoing adapters in module '"
                                + arch.contextName(source)
                                + "' must not access another module's internals - use its api/ or"
                                + " events/ packages instead"));
              }
              CollectedViolations.check(perModule, arch.classes());
            })
        .selecting(
            "Classes in <module>.adapter.outgoing.. of every isolated module root - every package"
                + " below the base package that owns a domain, application or adapter package,"
                + " declared as a bounded context or not, the shared kernel excluded. A module that"
                + " is the only isolated root has no foreign target and is skipped.")
        .checking(
            "No selected class depends on a class in another isolated module root (<other>..)"
                + " unless that class lives in the other module's published packages <other>.api.."
                + " or <other>.events.. (segment names from the layout). The other module's domain,"
                + " application, adapter and infrastructure packages are internal and reported. The"
                + " allow-list is the package convention alone - no framework annotation is read."
                + " Dependencies on the shared kernel and on third-party code are not checked.");
  }

  /** DCA-STR-007. */
  public DcaRule integrationEventsResideInEventsPackages() {
    return DcaRule.of(
            "DCA-STR-007",
            "Integration Events must be in events or adapter outgoing event packages",
            "Integration Events must be in events/ packages (published named interface) or"
                + " adapter.outgoing.event/ packages",
            arch ->
                classes()
                    .that()
                    .implement(IntegrationEvent.class)
                    .should()
                    .resideInAnyPackage(
                        ".." + layout.eventsSubpackage() + "..", outgoingEventAdapterPattern())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes assignable to IntegrationEvent - directly or through a"
                + " sub-interface - anywhere on the classpath under scan. Interfaces that extend"
                + " IntegrationEvent are not selected.")
        .checking(
            "Each resides in a package whose path contains the configured events segment"
                + " (..events..) or in ..adapter.outgoing.event.. - the trailing event segment is"
                + " fixed, not configurable. An integration event in a domain or application"
                + " package is reported.");
  }

  private String outgoingEventAdapterPattern() {
    return ".." + layout.adapterSubpackage() + "." + layout.outgoingSubpackage() + ".event..";
  }

  /** DCA-STR-008. */
  public static DcaRule integrationEventsAreRecords() {
    return DcaRule.of(
            "DCA-STR-008",
            "Integration Events should be immutable records",
            "Integration Events must be immutable to ensure event integrity across contexts (Event"
                + " Sourcing best practice)",
            arch ->
                classes()
                    .that()
                    .implement(IntegrationEvent.class)
                    .should()
                    .beRecords()
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes assignable to IntegrationEvent - directly or through a"
                + " sub-interface - anywhere on the classpath under scan.")
        .checking(
            "The class is a record. A final class with final fields does not count - only the"
                + " record form is accepted. The components' own immutability is not checked.");
  }

  /** DCA-STR-009. */
  public static DcaRule antiCorruptionLayerComponentsResideInAclPackages() {
    return DcaRule.of(
            "DCA-STR-009",
            "Anti-Corruption Layer components must be in acl packages",
            "Anti-Corruption Layer components must be in 'acl' packages for clear architectural intent"
                + " (DDD Strategic Pattern)",
            arch ->
                classes()
                    .that()
                    .haveSimpleNameEndingWith("EventTranslator")
                    .or()
                    .haveSimpleNameEndingWith("ACL")
                    .or()
                    .haveSimpleNameEndingWith("AntiCorruptionLayer")
                    .should()
                    .resideInAPackage("..acl..")
                    .allowEmptyShould(true))
        .selecting(
            "Classes anywhere on the classpath under scan whose simple name ends with"
                + " EventTranslator, ACL or AntiCorruptionLayer - selected by name alone, no marker"
                + " or annotation is read.")
        .checking(
            "Each resides in a package whose path contains an acl segment (..acl..), at any depth."
                + " A translation class named otherwise is neither selected nor checked.");
  }

  /** DCA-STR-010 — documentation only, never fails. */
  public static DcaRule eventListenersUseAntiCorruptionLayer() {
    return DcaRule.check(
            "DCA-STR-010",
            "Event Listeners consuming integration events should use Anti-Corruption Layer",
            "Consumed integration events are translated into the consuming context's own language"
                + " before they reach its domain — verified by code review, not statically",
            arch -> {})
        .selecting("Informational - selects nothing and never fails; it carries doctrine only.")
        .checking(
            "Nothing is asserted. Whether a consumed integration event is translated into the"
                + " consuming context's own language before it reaches the domain is a code-review"
                + " check.");
  }
}
