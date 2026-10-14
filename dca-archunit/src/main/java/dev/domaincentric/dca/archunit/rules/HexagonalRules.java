package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaMarkers;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.FrameworkAnnotations;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;
import java.util.ArrayList;
import java.util.List;

/**
 * Hexagonal Architecture (Ports and Adapters) rules: separation between ports and adapters,
 * incoming adapters drive the application through its input ports, outgoing adapters implement
 * outbound ports, adapters never talk to each other directly, incoming adapters stay inside their
 * own bounded context and do not operate the domain through its services, and outgoing adapters
 * never drive the application.
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
            incomingAdaptersMustNotDependOnDomainServices(),
            outgoingAdaptersMustNotDependOnInputPorts(),
            getHandlersDriveNoCommand(),
            incomingAdaptersKeepNoInstanceLocalState());
  }

  @Override
  public String name() {
    return "hexagonal";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  /**
   * Pattern of event consumers - the incoming adapters that react to other modules' integration
   * events; every segment comes from the layout.
   */
  private String eventConsumerPattern() {
    return layout.incomingEventAdapterPattern();
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
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.domain.model.. of every module root - the domain model"
                + " package, not the whole domain layer.")
        .checking(
            "No dependency on a class in <module>.adapter.. of any module root, incoming or"
                + " outgoing. Domain classes outside the model package (domain services, events) are"
                + " not selected. An empty selection passes.");
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
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.application.. of every module root, application.shared"
                + " included.")
        .checking(
            "No dependency on a class in <module>.adapter.. of any module root. An empty"
                + " selection passes.");
  }

  public DcaRule controllersMustNotAccessRepositories() {
    return DcaRule.of(
            "DCA-HEX-003",
            "Controllers and Resources must never access repositories directly",
            "Controllers must go through use cases (input ports), never directly to repositories",
            arch ->
                noClasses()
                    .that()
                    .haveSimpleNameEndingWith(layout.controllerSuffix())
                    .or()
                    .haveSimpleNameEndingWith(layout.restControllerSuffix())
                    .or(
                        AnnotationRoles.annotatedWithAny(
                            layout.frameworkAnnotations().webController(),
                            layout.frameworkAnnotations().restController()))
                    .should()
                    .dependOnClassesThat()
                    .areAssignableTo(arch.layout().markers().repository())
                    .allowEmptyShould(true))
        .selecting(
            "Classes anywhere on the classpath under scan whose simple name ends with the"
                + " configured controller suffix or with the configured REST-controller suffix."
                + " Also selected by configured web-controller or REST-controller role annotation; not restricted to adapter packages.")
        .checking(
            "No dependency on a class assignable to Repository - the port interface or an"
                + " implementation. Other output ports (Store, event publishers) are not checked; a"
                + " controller that reaches a repository through another class is not reported. An"
                + " empty selection passes.");
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
                    .allowEmptyShould(true))
        .selecting("Classes in <module>.adapter.incoming.. of every module root.")
        .checking(
            "No dependency on a class in an infrastructure package: the global"
                + " base.infrastructure or an isolated module's own <module>.infrastructure, the"
                + " package itself or any sub-package with an exact segment boundary. The shared"
                + " kernel's infrastructure package does not count as an infrastructure"
                + " implementation. An empty selection passes.");
  }

  public DcaRule outgoingAdaptersMustNotUseInfrastructureImplementations() {
    return DcaRule.check(
            "DCA-HEX-005",
            "Outgoing adapters must not use another module's infrastructure",
            "Technical infrastructure reuse preserves module isolation",
            arch -> {
              CollectedViolations violations =
                  CollectedViolations.withHeader(
                      "Outgoing adapters must not use another module's infrastructure\nbecause"
                          + " technical infrastructure reuse preserves module isolation");
              for (var adapter : arch.classes()) {
                if (!com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage(
                        arch.allOutgoingAdapterPatterns())
                    .test(adapter)) continue;
                String owner = arch.moduleRootOf(adapter.getPackageName());
                for (var dependency : adapter.getDirectDependenciesFromSelf()) {
                  var target = dependency.getTargetClass();
                  String targetOwner =
                      arch.isolatedModuleRoots().stream()
                          .filter(
                              root ->
                                  target.getPackageName().equals(layout.infrastructurePackage(root))
                                      || target
                                          .getPackageName()
                                          .startsWith(layout.infrastructurePackage(root) + "."))
                          .findFirst()
                          .orElse(layout.basePackage());
                  if (arch.infrastructureImplementation().test(target)
                      && !java.util.Objects.equals(owner, targetOwner)
                      && !layout.basePackage().equals(targetOwner))
                    violations.add(dependency.getDescription());
                }
              }
              violations.throwIfAny();
            })
        .selecting("Classes in every module's outgoing adapter package.")
        .checking(
            "Dependencies on global infrastructure and the adapter's own module infrastructure pass; infrastructure of another module fails. Module boundaries use exact package segments.");
  }

  public DcaRule adaptersMustNotCommunicateDirectly() {
    return DcaRule.of(
            "DCA-HEX-006",
            "Incoming port adapters must not depend directly on outgoing port adapters"
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
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.adapter.incoming.. of every module root, excluding those"
                + " below the configured event-consumer sub-package (adapter.incoming.event by"
                + " default).")
        .checking(
            "No dependency on a class in <module>.adapter.outgoing.. of any module root. The"
                + " reverse direction (an outgoing adapter using an incoming one) and dependencies"
                + " between two incoming or two outgoing adapters are not checked. An empty"
                + " selection passes.");
  }

  public DcaRule incomingAdaptersStayInOwnContext() {
    return DcaRule.check(
            "DCA-HEX-007",
            "Incoming adapters depend on no other module, except event consumers on the events"
                + " they subscribe to",
            "An incoming adapter drives its own module only: a controller that needs a sibling's"
                + " data goes through its own use case, output port and outgoing adapter to the"
                + " sibling's published api - only a subscription legitimately meets a foreign"
                + " contract on the incoming side",
            arch -> {
              // Structural, over every module that owns a DCA layer - declared as a bounded context
              // or not - so an undeclared module can neither reach out nor be reached into.
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
                                + "' drive their own module only - reach a sibling through an"
                                + " own use case, output port and outgoing adapter"));
              }
              CollectedViolations.check(perModule, arch.classes());
            })
        .selecting(
            "Per isolated module root - every module root except the shared kernel, declared"
                + " a bounded context or not: classes in <module>.adapter.incoming.., excluding"
                + " those below the configured event-consumer sub-package (adapter.incoming.event"
                + " by default), which are the exemption. A module that is the only isolated module"
                + " is skipped.")
        .checking(
            "No dependency on any class in another isolated module root (<other>..), its"
                + " published api and events packages included - the allow-list DCA-STR-006 grants"
                + " outgoing adapters does not apply on the incoming side. Event consumers are not"
                + " selected and so may depend on the events they subscribe to. Dependencies on the"
                + " shared kernel and on packages outside every module root are not checked."
                + " Findings of all modules are collected and reported together; a module without"
                + " incoming adapters passes.");
  }

  public DcaRule repositoryClassesResideInOutgoingAdapter() {
    return DcaRule.of(
            "DCA-HEX-008",
            "Classes named *Repository must reside in the outgoing adapter package (name-based discovery of unmarked repositories)",
            "Repository implementations are secondary adapters (outgoing ports)",
            arch ->
                classes()
                    .that()
                    .haveSimpleNameEndingWith("Repository")
                    .and()
                    .areNotInterfaces()
                    .should()
                    .resideInAnyPackage(arch.allOutgoingAdapterPatterns())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan whose simple name"
                + " ends with Repository - implementations and abstract base classes alike. The"
                + " Repository port interfaces themselves are not selected.")
        .checking(
            "Each resides in <module>.adapter.outgoing.. of some module root. Whether the"
                + " class implements a Repository port is not checked - only the name is. An empty"
                + " selection passes.");
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
                    .beAssignableTo(arch.layout().markers().outputPort())
                    .allowEmptyShould(true))
        .selecting(
            "Top-level interfaces in <module>.application.<shared>.. of every module root,"
                + " excluding package-info. Nested interfaces are not selected - they belong to"
                + " their enclosing port's contract.")
        .checking(
            "The interface is assignable to OutputPort, directly or through Repository,"
                + " Store, DomainEventPublisher, IntegrationEventPublisher or another OutputPort"
                + " sub-interface. Classes, records and enums in application.shared are not checked."
                + " An empty selection passes. The rule presumes the vocabulary has a common port"
                + " root: withOutputPort must be set whenever any port role is, because every port"
                + " of the project is measured against it. A vocabulary that has no such root has"
                + " nothing this rule can assert - switch the id off rather than pointing the role"
                + " at an unrelated type.");
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
                    .dependOnClassesThat(useCaseImplementations(arch.layout().markers()))
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.adapter.incoming.. of every module root, event consumers"
                + " included.")
        .checking(
            "No dependency on a use case implementation: a non-interface class assignable to"
                + " InputPort, directly or through a *InputPort interface - abstract base classes"
                + " included. Depending on the input port interfaces themselves is what the rule"
                + " expects. An empty selection passes.");
  }

  /** A use case implementation: a class (never an interface) behind an {@link InputPort}. */
  private static DescribedPredicate<JavaClass> useCaseImplementations(DcaMarkers markers) {
    return new DescribedPredicate<>("are use case implementations rather than input ports") {
      @Override
      public boolean test(JavaClass javaClass) {
        return !javaClass.isInterface() && javaClass.isAssignableTo(markers.inputPort());
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
                    .areAssignableTo(arch.layout().markers().outputPort())
                    .and()
                    .areInterfaces()
                    .should()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .allowEmptyShould(true))
        .selecting(
            "Interfaces anywhere on the classpath under scan that are assignable to"
                + " OutputPort - the building-block port interfaces themselves included.")
        .checking(
            "None resides in <module>.domain.. of any module root. Classes implementing an"
                + " output port are not selected; where an interface outside the domain has to live"
                + " is not checked here. An empty selection passes.");
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
                    .areAssignableTo(arch.layout().markers().domainService())
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.adapter.incoming.. of every module root, event consumers"
                + " included. Outgoing adapters are not selected.")
        .checking(
            "No dependency on a class assignable to DomainService - the building-block marker"
                + " interface, any sub-interface of it and every class implementing one. A domain"
                + " class without the marker is not a domain service by this rule. Injecting it,"
                + " calling it or naming it in a signature all count as a dependency. An empty"
                + " selection passes.",
            "move the collaboration into the use case and carry its outcome in the result");
  }

  /**
   * The direction of an adapter is decided by who calls whom: an adapter that invokes an input port
   * drives the application and is incoming, whatever package it sits in.
   */
  public DcaRule outgoingAdaptersMustNotDependOnInputPorts() {
    return DcaRule.of(
            "DCA-HEX-013",
            "Outgoing Adapters must not depend on input ports",
            "An outgoing adapter is driven: the application calls it through an output port. One that"
                + " calls an input port drives the application instead - it is an incoming adapter"
                + " filed on the wrong side, and the call it makes runs a second use case from inside"
                + " the first one's outgoing edge",
            arch ->
                noClasses()
                    .that()
                    .resideInAnyPackage(arch.allOutgoingAdapterPatterns())
                    .should()
                    .dependOnClassesThat()
                    .areAssignableTo(arch.layout().markers().inputPort())
                    .allowEmptyShould(true))
        .selecting("Classes in <module>.adapter.outgoing.. of every module root.")
        .checking(
            "No dependency on a type assignable to InputPort - the input port interfaces and the use"
                + " case classes implementing them, of any module. Calling another module's published"
                + " api is not a dependency on an input port, even where that api delegates to one"
                + " - only direct dependencies are read. An empty selection passes.",
            "move the class to the incoming adapter package, or let the use case reach the"
                + " capability through an output port");
  }

  /**
   * DCA-HEX-014. The command is recognised by its input, as DCA-USE-002 and DCA-USE-004 recognise
   * it: the input port takes a type whose simple name ends with {@code Command}.
   */
  public DcaRule getHandlersDriveNoCommand() {
    List<String> controllers = layout.frameworkAnnotations().webController();
    List<String> getMapping = layout.frameworkAnnotations().getMapping();
    return DcaRule.of(
            "DCA-HEX-014",
            "GET handlers must not drive an input port that takes a command",
            "A state-changing use case is reached only by an unsafe HTTP method, never by GET."
                + " Links, prefetching, crawlers and cross-site navigation issue GET requests on"
                + " their own; a GET that runs a command changes state nobody asked to change",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allIncomingAdapterPatterns())
                    .and(AnnotationRoles.annotatedWithAny(controllers))
                    .should(driveNoCommandFromGet(getMapping, layout.markers()))
                    .allowEmptyShould(true))
        .experimental()
        .selecting(
            "Classes in <module>.adapter.incoming.. of every module root that are directly"
                + " annotated with one of the configured web-controller stereotypes; within them,"
                + " the methods carrying, directly or as a meta-annotation, an annotation of the"
                + " configured GET-mapping role.")
        .checking(
            "No such method - or a method of the same class it reaches through calls within the"
                + " class - calls a type assignable to the configured input-port role whose input"
                + " ends with Command: a parameter of the called method, of a public method of the"
                + " port, or a type argument of the input port it extends. A mapping that names its"
                + " HTTP method in an attribute is not read. With an empty web-controller or"
                + " GET-mapping role nothing is selected and the rule passes.",
            "map the handler to POST, PUT or DELETE, or let the GET handler ask a query");
  }

  private static ArchCondition<JavaClass> driveNoCommandFromGet(
      List<String> getMapping, DcaMarkers markers) {
    return new ArchCondition<>("drive no command from a GET handler") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        IntraClassCalls calls = new IntraClassCalls(item);
        for (JavaCodeUnit handler : item.getCodeUnits()) {
          if (!AnnotationRoles.isMetaAnnotatedWithAny(handler, getMapping)) {
            continue;
          }
          java.util.Set<String> commands = new java.util.TreeSet<>();
          for (JavaCodeUnit unit : calls.reachableFrom(handler)) {
            for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
              JavaClass owner = call.getTargetOwner();
              if (owner.isAssignableTo(markers.inputPort()) && takesCommand(call, markers)) {
                commands.add(owner.getSimpleName() + "." + call.getName());
              }
            }
          }
          if (!commands.isEmpty()) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getName()
                        + "."
                        + handler.getName()
                        + " handles "
                        + FrameworkAnnotations.describe(getMapping, "a GET request")
                        + " and drives a command through "
                        + String.join(", ", commands)));
          }
        }
      }
    };
  }

  private static boolean takesCommand(JavaMethodCall call, DcaMarkers markers) {
    return call.getTarget().getRawParameterTypes().stream()
            .anyMatch(parameter -> parameter.getSimpleName().endsWith("Command"))
        || TypeInspection.takesInputEndingWith(
            call.getTargetOwner(), markers.inputPort(), "Command");
  }

  public DcaRule incomingAdaptersKeepNoInstanceLocalState() {
    List<String> api = layout.frameworkAnnotations().instanceLocalState();
    return DcaRule.of(
            "DCA-HEX-015",
            "Incoming adapters must not keep state for a later request in one instance",
            "State a later request needs travels with that request or lives in a store every"
                + " instance reads. A server session or a message held for the next request is kept"
                + " by one instance, and behind a load balancer without sticky sessions the next"
                + " request reaches another one. A message for the page after a redirect is a fixed"
                + " code in the redirect address, which the page maps to its text",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allIncomingAdapterPatterns())
                    .should(callNoInstanceLocalState(api))
                    .allowEmptyShould(true))
        .experimental()
        .selecting("Classes in <module>.adapter.incoming.. of every module root.")
        .checking(
            "No code unit calls an entry of the configured instance-local-state role: an entry"
                + " naming a type matches a call of any method on a type assignable to it, an entry"
                + " whose last segment starts in lower case (Type.method) a call of that method"
                + " only. Holding such a type as a parameter or field without calling it is not"
                + " reported, nor is state kept through an annotation. With an empty role nothing is"
                + " reported.",
            "carry the state in the request - a fixed code in the redirect address - or keep it"
                + " in a store every instance reads");
  }

  private static ArchCondition<JavaClass> callNoInstanceLocalState(List<String> api) {
    return new ArchCondition<>("call no instance-local state API") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        for (JavaCodeUnit unit : item.getCodeUnits()) {
          java.util.Set<String> used = new java.util.TreeSet<>();
          for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
            for (String entry : api) {
              if (matchesInstanceLocalState(call, entry)) {
                used.add(call.getTargetOwner().getSimpleName() + "." + call.getName());
              }
            }
          }
          if (!used.isEmpty()) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getName()
                        + "."
                        + unit.getName()
                        + " keeps request state in this instance through "
                        + String.join(", ", used)));
          }
        }
      }
    };
  }

  /**
   * {@code com.acme.Session} matches every call on a type assignable to it; {@code
   * com.acme.Redirect.keep} - the last segment in lower case - only calls of {@code keep}.
   */
  private static boolean matchesInstanceLocalState(JavaMethodCall call, String entry) {
    int dot = entry.lastIndexOf('.');
    String last = entry.substring(dot + 1);
    if (dot > 0 && !last.isEmpty() && Character.isLowerCase(last.charAt(0))) {
      return call.getName().equals(last)
          && call.getTargetOwner().isAssignableTo(entry.substring(0, dot));
    }
    return call.getTargetOwner().isAssignableTo(entry);
  }
}
