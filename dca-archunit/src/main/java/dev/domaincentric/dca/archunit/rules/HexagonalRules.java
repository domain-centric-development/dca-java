package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.buildingblocks.ddd.strategic.BoundedContext;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import java.util.List;
import java.util.Map;

/**
 * Hexagonal Architecture (Ports and Adapters) rules: separation between ports and adapters,
 * incoming adapters drive the application, outgoing adapters implement outbound ports, adapters
 * never talk to each other directly, incoming adapters stay inside their own bounded context.
 */
public final class HexagonalRules implements DcaRuleSet {

  private final DcaLayout layout;
  private final List<DcaRule> rules;

  public HexagonalRules(DcaLayout layout) {
    this.layout = layout;
    this.rules =
        List.of(
            domainMustNotAccessAdapters(),
            applicationMustNotAccessAdapters(),
            controllersMustNotAccessRepositories(),
            incomingAdaptersMustNotUseInfrastructureImplementations(),
            outgoingAdaptersMustNotUseInfrastructureImplementations(),
            adaptersMustNotCommunicateDirectly(),
            incomingAdaptersStayInOwnContext(),
            repositoryClassesResideInOutgoingAdapter(),
            sharedOutputPortsExtendOutputPort(),
            outputPortsMustNotResideInDomain());
  }

  @Override
  public String name() {
    return "hexagonal";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  /** Pattern of event consumers, which may depend on other contexts' integration events. */
  private String eventConsumerPattern() {
    return ".." + layout.adapterSubpackage() + "." + layout.incomingSubpackage() + ".event..";
  }

  public DcaRule domainMustNotAccessAdapters() {
    return DcaRule.of(
        "DCA-HEX-001",
        "Classes from the domain should not access port adapters",
        "Domain should not depend on adapters (ports and adapters pattern)",
        arch ->
            noClasses()
                .that()
                .resideInAPackage(layout.domainModelPattern())
                .should()
                .dependOnClassesThat()
                .resideInAPackage(layout.adapterPattern()));
  }

  public DcaRule applicationMustNotAccessAdapters() {
    return DcaRule.of(
        "DCA-HEX-002",
        "Application Services should not access port adapters",
        "Application services should only depend on domain and outbound ports, not adapters",
        arch ->
            noClasses()
                .that()
                .resideInAPackage(layout.applicationPattern())
                .should()
                .dependOnClassesThat()
                .resideInAPackage(layout.adapterPattern()));
  }

  public DcaRule controllersMustNotAccessRepositories() {
    return DcaRule.of(
        "DCA-HEX-003",
        "Controllers and Resources must never access repositories directly",
        "Controllers must go through use cases (input ports), never directly to repositories",
        arch ->
            noClasses()
                .that()
                .haveSimpleNameEndingWith("Controller")
                .or()
                .haveSimpleNameEndingWith(layout.restControllerSuffix())
                .should()
                .dependOnClassesThat()
                .areAssignableTo(Repository.class)
                .allowEmptyShould(true));
  }

  public DcaRule incomingAdaptersMustNotUseInfrastructureImplementations() {
    return DcaRule.of(
        "DCA-HEX-004",
        "Incoming Adapters must only use outbound ports (not infrastructure implementations)",
        "Incoming adapters should only use outbound ports declared as interfaces (port.out), not"
            + " infrastructure implementation details",
        arch ->
            noClasses()
                .that()
                .resideInAPackage(layout.incomingAdapterPattern())
                .should()
                .dependOnClassesThat(arch.infrastructureImplementation()));
  }

  public DcaRule outgoingAdaptersMustNotUseInfrastructureImplementations() {
    return DcaRule.of(
        "DCA-HEX-005",
        "Outgoing Adapters must only use outbound ports (not infrastructure implementations)",
        "Outgoing adapters should only use outbound ports declared as interfaces (port.out), not"
            + " infrastructure implementation details",
        arch ->
            noClasses()
                .that()
                .resideInAPackage(layout.outgoingAdapterPattern())
                .should()
                .dependOnClassesThat(arch.infrastructureImplementation()));
  }

  public DcaRule adaptersMustNotCommunicateDirectly() {
    return DcaRule.of(
        "DCA-HEX-006",
        "Port adapters (incoming and outgoing) must not communicate directly with each other"
            + " within the same context",
        "Port adapters should communicate through application services, not directly (event"
            + " consumers are the exception)",
        arch ->
            noClasses()
                .that()
                .resideInAPackage(layout.incomingAdapterPattern())
                .and()
                .resideOutsideOfPackage(eventConsumerPattern())
                .should()
                .dependOnClassesThat()
                .resideInAPackage(layout.outgoingAdapterPattern()));
  }

  public DcaRule incomingAdaptersStayInOwnContext() {
    return DcaRule.check(
        "DCA-HEX-007",
        "Incoming adapters must only access their own bounded context (except event consumers and"
            + " Open Host Services)",
        "Incoming adapters must only orchestrate use cases from their own bounded context - use"
            + " domain events for cross-context integration",
        arch -> {
          Map<String, BoundedContext> contexts = arch.boundedContexts();
          for (Map.Entry<String, BoundedContext> entry : contexts.entrySet()) {
            String contextPackage = entry.getKey();
            String[] otherContexts = arch.boundedContextPatternsExcluding(contextPackage);
            if (otherContexts.length == 0) {
              continue;
            }
            noClasses()
                .that()
                .resideInAPackage(layout.incomingAdapterPattern(contextPackage))
                .and()
                .resideOutsideOfPackage(eventConsumerPattern())
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(otherContexts)
                .allowEmptyShould(true)
                .because(
                    "Incoming adapters in '"
                        + entry.getValue().name()
                        + "' must only orchestrate use cases from their own bounded context - use"
                        + " domain events for cross-context integration")
                .check(arch.classes());
          }
        });
  }

  public DcaRule repositoryClassesResideInOutgoingAdapter() {
    return DcaRule.of(
        "DCA-HEX-008",
        "Classes named *Repository must reside in the outgoing adapter package",
        "Repository implementations are secondary adapters (outgoing ports)",
        arch ->
            classes()
                .that()
                .haveSimpleNameEndingWith("Repository")
                .and()
                .areNotInterfaces()
                .should()
                .resideInAPackage(layout.outgoingAdapterPattern())
                .allowEmptyShould(true));
  }

  public DcaRule sharedOutputPortsExtendOutputPort() {
    return DcaRule.of(
        "DCA-HEX-009",
        "Output Ports in application.shared must extend OutputPort",
        "Top-level interfaces in application.shared are output ports and must extend OutputPort to"
            + " be part of the port hierarchy. Nested interfaces (e.g. IdentityProvider.Identity)"
            + " are part of their enclosing port's contract, not ports themselves",
        arch ->
            classes()
                .that()
                .resideInAPackage(layout.sharedOutputPortPattern())
                .and()
                .areInterfaces()
                .and()
                .areTopLevelClasses()
                .and()
                .haveSimpleNameNotEndingWith("package-info")
                .should()
                .beAssignableTo(OutputPort.class)
                .allowEmptyShould(true));
  }

  public DcaRule outputPortsMustNotResideInDomain() {
    return DcaRule.of(
        "DCA-HEX-010",
        "Output ports must not reside in the domain layer",
        "output ports (Repository, Store, OutputPort) are an application-layer concern and must live"
            + " in application/shared/, not domain/",
        arch ->
            noClasses()
                .that()
                .areAssignableTo(OutputPort.class)
                .and()
                .areInterfaces()
                .should()
                .resideInAPackage(layout.domainPattern())
                .allowEmptyShould(true));
  }
}
