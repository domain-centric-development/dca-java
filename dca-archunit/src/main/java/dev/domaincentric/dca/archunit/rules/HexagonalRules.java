package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchRule;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainService;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import java.util.ArrayList;
import java.util.List;

/**
 * Hexagonal Architecture (Ports and Adapters) rules: separation between ports and adapters,
 * incoming adapters drive the application through its input ports, outgoing adapters implement
 * outbound ports, adapters never talk to each other directly, incoming adapters stay inside their
 * own bounded context and do not operate the domain through its services.
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
            outputPortsMustNotResideInDomain(),
            incomingAdaptersMustDependOnInputPortsNotUseCaseClasses(),
            incomingAdaptersMustNotDependOnDomainServices());
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
                .resideInAnyPackage(arch.allDomainModelPatterns())
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(arch.allAdapterPatterns())
                .allowEmptyShould(true));
  }

  public DcaRule applicationMustNotAccessAdapters() {
    return DcaRule.of(
        "DCA-HEX-002",
        "Application Services should not access port adapters",
        "Application services should only depend on domain and outbound ports, not adapters",
        arch ->
            noClasses()
                .that()
                .resideInAnyPackage(arch.allApplicationPatterns())
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(arch.allAdapterPatterns())
                .allowEmptyShould(true));
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
                .resideInAnyPackage(arch.allIncomingAdapterPatterns())
                .should()
                .dependOnClassesThat(arch.infrastructureImplementation())
                .allowEmptyShould(true));
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
                .resideInAnyPackage(arch.allOutgoingAdapterPatterns())
                .should()
                .dependOnClassesThat(arch.infrastructureImplementation())
                .allowEmptyShould(true));
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
                .resideInAnyPackage(arch.allIncomingAdapterPatterns())
                .and()
                .resideOutsideOfPackage(eventConsumerPattern())
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(arch.allOutgoingAdapterPatterns())
                .allowEmptyShould(true));
  }

  public DcaRule incomingAdaptersStayInOwnContext() {
    return DcaRule.check(
        "DCA-HEX-007",
        "Incoming adapters must only access their own bounded context (except event consumers and"
            + " Open Host Services)",
        "Incoming adapters must only orchestrate use cases from their own bounded context - use"
            + " domain events for cross-context integration",
        arch -> {
          // Structural, over every module that owns a DCA layer - declared as a bounded context or
          // not - so an undeclared module can neither reach out nor be reached into.
          List<ArchRule> perModule = new ArrayList<>();
          for (String module : arch.isolatedModuleRoots()) {
            String[] otherModules = arch.moduleRootPatternsExcluding(module);
            if (otherModules.length == 0) {
              continue;
            }
            perModule.add(
                noClasses()
                    .that()
                    .resideInAPackage(layout.incomingAdapterPattern(module))
                    .and()
                    .resideOutsideOfPackage(eventConsumerPattern())
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(otherModules)
                    .allowEmptyShould(true)
                    .because(
                        "Incoming adapters in module '"
                            + arch.contextName(module)
                            + "' must only orchestrate use cases from their own module - use"
                            + " domain events for cross-context integration"));
          }
          CollectedViolations.check(perModule, arch.classes());
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
                .resideInAnyPackage(arch.allOutgoingAdapterPatterns())
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
                .resideInAnyPackage(arch.allSharedOutputPortPatterns())
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

  public DcaRule incomingAdaptersMustDependOnInputPortsNotUseCaseClasses() {
    return DcaRule.of(
        "DCA-HEX-011",
        "Incoming Adapters must depend on input port interfaces, not on use case classes",
        "A driving adapter drives the application through its port. Injecting the concrete"
            + " implementation instead couples the adapter to one realisation of the use case,"
            + " defeats the Dependency Inversion Principle the port exists for, and makes the"
            + " adapter untestable without the real use case and everything it depends on",
        arch ->
            noClasses()
                .that()
                .resideInAnyPackage(arch.allIncomingAdapterPatterns())
                .should()
                .dependOnClassesThat(useCaseImplementations())
                .allowEmptyShould(true));
  }

  /** A use case implementation: a class (never an interface) behind an {@link InputPort}. */
  private static DescribedPredicate<JavaClass> useCaseImplementations() {
    return new DescribedPredicate<>("are use case implementations rather than input ports") {
      @Override
      public boolean test(JavaClass javaClass) {
        return !javaClass.isInterface() && javaClass.isAssignableTo(InputPort.class);
      }
    };
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
                .resideInAnyPackage(arch.allDomainPatterns())
                .allowEmptyShould(true));
  }

  /**
   * Selects incoming adapters only, event consumers included: they translate and call an input port
   * like every other driving adapter. Outgoing adapters are deliberately outside the selection.
   */
  public DcaRule incomingAdaptersMustNotDependOnDomainServices() {
    return DcaRule.of(
        "DCA-HEX-012",
        "Incoming Adapters must not depend on domain services",
        "An incoming adapter translates external input, calls an input port and formats its result."
            + " Injecting or invoking a domain service bypasses the application boundary; the use"
            + " case owns that collaboration and puts its outcome into the result. Outgoing adapters"
            + " are outside this rule - repositories and other driven adapters may construct or"
            + " reconstitute domain objects while implementing output ports",
        arch ->
            noClasses()
                .that()
                .resideInAnyPackage(arch.allIncomingAdapterPatterns())
                .should()
                .dependOnClassesThat()
                .areAssignableTo(DomainService.class)
                .allowEmptyShould(true));
  }
}
