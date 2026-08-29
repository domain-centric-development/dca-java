package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.OpenHostService;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import java.util.List;
import java.util.Map;

/**
 * DDD strategic pattern rules: shared-kernel independence, bounded-context isolation, Open Host
 * Services, Integration Events and Anti-Corruption Layers. Bounded contexts and the shared kernel
 * are discovered via {@code @BoundedContext} / {@code @SharedKernel} on {@code package-info}.
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
            openHostServicesResideInApiOrOpenHostPackages(),
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
        });
  }

  /** DCA-STR-002. */
  public static DcaRule sharedKernelMustNotDependOnBoundedContexts() {
    return DcaRule.check(
        "DCA-STR-002",
        "Shared Kernel must not have dependencies on any bounded context",
        "Shared Kernel must be context-independent — it is shared by all contexts and owned by"
            + " none",
        arch ->
            arch.sharedKernelPackage()
                .ifPresent(
                    sharedKernel -> {
                      for (Map.Entry<String, BoundedContext> ctx :
                          arch.boundedContexts().entrySet()) {
                        noClasses()
                            .that()
                            .resideInAPackage(sharedKernel + "..")
                            .should()
                            .dependOnClassesThat()
                            .resideInAPackage(ctx.getKey() + "..")
                            .allowEmptyShould(true)
                            .because(
                                "Shared Kernel must not depend on bounded context '"
                                    + ctx.getValue().name()
                                    + "' ("
                                    + ctx.getKey()
                                    + ") - Shared Kernel must be context-independent")
                            .check(arch.classes());
                      }
                    }));
  }

  /** DCA-STR-003. */
  public DcaRule applicationLayerIsolation() {
    return DcaRule.check(
        "DCA-STR-003",
        "Bounded contexts must not directly access each other in application layer (except allowed"
            + " dependencies)",
        "Application layers talk to other contexts through output ports and adapters, never"
            + " directly",
        arch -> {
          for (Map.Entry<String, BoundedContext> source : arch.boundedContexts().entrySet()) {
            String[] forbidden = arch.boundedContextPatternsExcluding(source.getKey());
            if (forbidden.length == 0) {
              continue;
            }
            // dependOnClassesThat, not accessClassesThat: "access" is a method call or field
            // access, so a field, parameter or record component of a foreign type slips past it.
            noClasses()
                .that()
                .resideInAPackage(layout.applicationPattern(source.getKey()))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(forbidden)
                .allowEmptyShould(true)
                .because(
                    "Application layer of bounded context '"
                        + source.getValue().name()
                        + "' must not access other contexts directly - define output ports and"
                        + " use adapters instead")
                .check(arch.classes());
          }
        });
  }

  /** DCA-STR-004. */
  public DcaRule domainLayerIsolation() {
    return DcaRule.check(
        "DCA-STR-004",
        "Bounded contexts must not access each other in the domain layer",
        "A domain layer talks to its own context and the shared kernel, nothing else — not even"
            + " another context's api/",
        arch -> {
          for (Map.Entry<String, BoundedContext> source : arch.boundedContexts().entrySet()) {
            String[] forbidden = arch.boundedContextPatternsExcluding(source.getKey());
            if (forbidden.length == 0) {
              continue;
            }
            noClasses()
                .that()
                .resideInAPackage(layout.domainPattern(source.getKey()))
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(forbidden)
                .allowEmptyShould(true)
                .because(
                    "The domain layer of bounded context '"
                        + source.getValue().name()
                        + "' must depend on nothing outside its own context and the shared kernel")
                .check(arch.classes());
          }
        });
  }

  /** DCA-STR-005. */
  public DcaRule openHostServicesResideInApiOrOpenHostPackages() {
    return DcaRule.of(
        "DCA-STR-005",
        "Open Host Services must reside in api or adapter.incoming.openhost packages",
        "Open Host Services expose context capabilities via api/ packages (published named"
            + " interface) or adapter.incoming.openhost/ packages",
        arch ->
            classes()
                .that()
                .areAnnotatedWith(OpenHostService.class)
                .should()
                .resideInAnyPackage("..api..", openHostAdapterPattern())
                .allowEmptyShould(true));
  }

  private String openHostAdapterPattern() {
    return ".." + layout.adapterSubpackage() + "." + layout.incomingSubpackage() + ".openhost..";
  }

  /** DCA-STR-006. */
  public DcaRule outgoingAdaptersOnlyUseOpenHostServices() {
    return DcaRule.check(
        "DCA-STR-006",
        "Outgoing adapters accessing other contexts must only use OpenHostService classes (except"
            + " allowed ACL patterns)",
        "Cross-context communication goes through the published api/ and events/ packages, never"
            + " through another context's domain or application layer",
        arch -> {
          Map<String, BoundedContext> contexts = arch.boundedContexts();
          for (Map.Entry<String, BoundedContext> source : contexts.entrySet()) {
            for (Map.Entry<String, BoundedContext> target : contexts.entrySet()) {
              if (target.getKey().equals(source.getKey())) {
                continue;
              }
              noClasses()
                  .that()
                  .resideInAPackage(layout.outgoingAdapterPattern(source.getKey()))
                  .should()
                  .dependOnClassesThat()
                  .resideInAPackage(layout.domainPattern(target.getKey()))
                  .allowEmptyShould(true)
                  .because(
                      "Outgoing adapters in '"
                          + source.getValue().name()
                          + "' must not access domain layer of '"
                          + target.getValue().name()
                          + "' - use api/ or events/ packages instead")
                  .check(arch.classes());
              noClasses()
                  .that()
                  .resideInAPackage(layout.outgoingAdapterPattern(source.getKey()))
                  .should()
                  .dependOnClassesThat()
                  .resideInAPackage(layout.applicationPattern(target.getKey()))
                  .allowEmptyShould(true)
                  .because(
                      "Outgoing adapters in '"
                          + source.getValue().name()
                          + "' must not access application layer of '"
                          + target.getValue().name()
                          + "' - use api/ or events/ packages instead")
                  .check(arch.classes());
            }
          }
        });
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
                .resideInAnyPackage("..events..", outgoingEventAdapterPattern())
                .allowEmptyShould(true));
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
                .allowEmptyShould(true));
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
                .allowEmptyShould(true));
  }

  /** DCA-STR-010 — documentation only, never fails. */
  public static DcaRule eventListenersUseAntiCorruptionLayer() {
    return DcaRule.check(
        "DCA-STR-010",
        "Event Listeners consuming integration events should use Anti-Corruption Layer",
        "Consumed integration events are translated into the consuming context's own language"
            + " before they reach its domain — verified by code review, not statically",
        arch -> {});
  }
}
