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
import com.tngtech.archunit.lang.conditions.ArchConditions;
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
            adaptersPublishNoEnumMemberName(),
            listenersThatDriveAnInputPortRunAfterTheCommit(),
            controllersResideInIncomingAdapters(),
            outputPortImplementationsResideInOutgoingAdapters());
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
                    .that(arch.inDomainModel())
                    .should()
                    .dependOnClassesThat(arch.inAdapter())
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
                    .that(arch.inApplication())
                    .should()
                    .dependOnClassesThat(arch.inAdapter())
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
                    .dependOnClassesThat(arch.layout().markers().is("repository"))
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
                    .that(arch.inIncomingAdapter())
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
                if (!arch.inOutgoingAdapter().test(adapter)) continue;
                String owner = arch.moduleRootOf(adapter);
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
                    .that(arch.inIncomingAdapter())
                    .and()
                    .resideOutsideOfPackage(eventConsumerPattern())
                    .should()
                    .dependOnClassesThat(arch.inOutgoingAdapter())
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
                        .that(arch.inLayerOf(DcaLayout.Layer.INCOMING_ADAPTER, module))
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
                    .should(ArchConditions.be(arch.inOutgoingAdapter()))
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
                    .that(arch.inSharedOutputPorts())
                    .and()
                    .areInterfaces()
                    .and()
                    .areTopLevelClasses()
                    .and()
                    .haveSimpleNameNotEndingWith("package-info")
                    .should(ArchConditions.be(arch.layout().markers().is("outputPort")))
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
                    .that(arch.inIncomingAdapter())
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
        return !javaClass.isInterface() && markers.is("inputPort").test(javaClass);
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
                    .that(arch.layout().markers().is("outputPort"))
                    .and()
                    .areInterfaces()
                    .should(ArchConditions.be(arch.inDomain()))
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
                    .that(arch.inIncomingAdapter())
                    .should()
                    .dependOnClassesThat(arch.layout().markers().is("domainService"))
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
                    .that(arch.inOutgoingAdapter())
                    .should()
                    .dependOnClassesThat(arch.layout().markers().is("inputPort"))
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
    List<String> webControllers = layout.frameworkAnnotations().webController();
    List<String> restControllers = layout.frameworkAnnotations().restController();
    List<String> getMapping = layout.frameworkAnnotations().getMapping();
    return DcaRule.of(
            "DCA-HEX-014",
            "GET handlers must not drive an input port that takes a command",
            "A state-changing use case is reached only by an unsafe HTTP method, never by GET."
                + " Links, prefetching, crawlers and cross-site navigation issue GET requests on"
                + " their own; a GET that runs a command changes state nobody asked to change. A"
                + " handler that names no HTTP method answers GET as well",
            arch ->
                classes()
                    .that(arch.inIncomingAdapter())
                    .and(AnnotationRoles.annotatedWithAny(webControllers, restControllers))
                    .should(driveNoCommandFromGet(getMapping, layout.markers()))
                    .allowEmptyShould(true))
        .experimental()
        .selecting(
            "Classes in <module>.adapter.incoming.. of every module root that are directly"
                + " annotated with one of the configured web-controller or REST-controller"
                + " stereotypes; within them,"
                + " the methods carrying, directly or as a meta-annotation, an annotation of the"
                + " configured GET-mapping role - or, for an entry that names an attribute value, an"
                + " annotation of that type whose attribute holds the value (a general mapping"
                + " annotation restricted to GET), and for an entry that names an attribute without a"
                + " value, an annotation of that type whose attribute holds none (a general mapping"
                + " that names no HTTP method). The values of the same annotation on the declaring"
                + " class count as well; a class-level mapping alone makes no method a handler. A"
                + " mapping that names no HTTP method answers every method, GET included, and counts"
                + " as a GET handler.")
        .checking(
            "No such method - or a method of the same class it reaches through calls within the"
                + " class - calls a type assignable to the configured input-port role whose input is"
                + " a command (the configured command role, by default a simple name ending with"
                + " Command): a parameter of the called method, of a public method of the port, or a"
                + " type argument of the input port it extends. A mapping that names no"
                + " HTTP method is checked like a GET handler. With empty web-controller and"
                + " REST-controller roles, or an empty GET-mapping role, nothing is selected and the"
                + " rule passes.",
            "map the handler to POST, PUT or DELETE, or let the GET handler ask a query");
  }

  private static ArchCondition<JavaClass> driveNoCommandFromGet(
      List<String> getMapping, DcaMarkers markers) {
    return new ArchCondition<>("drive no command from a GET handler") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        IntraClassCalls calls = new IntraClassCalls(item);
        for (JavaCodeUnit handler : item.getCodeUnits()) {
          String mapping = AnnotationRoles.matchingEntry(handler, getMapping);
          if (mapping == null) {
            continue;
          }
          java.util.Set<String> commands = new java.util.TreeSet<>();
          for (JavaCodeUnit unit : calls.reachableFrom(handler)) {
            for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
              JavaClass owner = call.getTargetOwner();
              if (markers.is("inputPort").test(owner) && takesCommand(call, markers)) {
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
                        + FrameworkAnnotations.describe(mapping)
                        + " and drives a command through "
                        + String.join(", ", commands)));
          }
        }
      }
    };
  }

  private static boolean takesCommand(JavaMethodCall call, DcaMarkers markers) {
    return call.getTarget().getRawParameterTypes().stream().anyMatch(markers.is("command"))
        || TypeInspection.takesInput(
            call.getTargetOwner(), markers.is("inputPort"), markers.is("command"));
  }

  /**
   * DCA-HEX-017. A reaction to an event drives a use case after the publishing transaction
   * committed, in a transaction of its own. A listener method is found through the event-listener
   * role; the call to an input port is followed through the class's own helpers.
   */
  public DcaRule listenersThatDriveAnInputPortRunAfterTheCommit() {
    FrameworkAnnotations roles = layout.frameworkAnnotations();
    return DcaRule.of(
            "DCA-HEX-017",
            "Event listeners that drive an input port must run after the commit in their own"
                + " transaction",
            "A reaction to an event changes state or performs an external effect through a use"
                + " case. Run inside the transaction that raised the event, it changes a second"
                + " aggregate in the first one's transaction, a failure in the reaction rolls back"
                + " the fact that caused it, and a retry of the request runs the reaction again. A"
                + " listener that only translates or measures calls output ports and may run in"
                + " that transaction",
            arch ->
                classes()
                    .should(driveAnInputPortOnlyAfterTheCommit(roles, layout.markers()))
                    .allowEmptyShould(true))
        .experimental()
        .selecting(
            "Classes of every module root; within them, the methods carrying, directly or as a"
                + " meta-annotation, an annotation of the configured event-listener role - on the"
                + " method or on one of its parameters, where CDI puts its observer. With an"
                + " empty event-listener or transactional-event-listener role nothing is selected"
                + " and the rule passes.")
        .checking(
            "A selected method that - itself or a method of the same class it reaches through"
                + " calls within the class - calls a type assignable to the configured input-port"
                + " role carries, directly or as a meta-annotation, an annotation of the configured"
                + " transactional-event-listener role - on the method or a parameter; an entry"
                + " <annotation>#<attribute>=<value> asks for that attribute value, as CDI's"
                + " observer phase - and an annotation of the configured"
                + " transactional role on the method or its class. The rule reads the listener's"
                + " phase and transaction, not whether a durable publication store backs it; an"
                + " input port reached through a second class is not followed. An empty selection"
                + " passes.",
            "deliver the reaction after the commit in its own transaction - a transactional event"
                + " listener backed by an outbox or an event publication registry - or let the"
                + " listener call only output ports");
  }

  private static ArchCondition<JavaClass> driveAnInputPortOnlyAfterTheCommit(
      FrameworkAnnotations roles, DcaMarkers markers) {
    return new ArchCondition<>("drive an input port only after the commit") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        if (roles.eventListener().isEmpty() || roles.transactionalEventListener().isEmpty()) {
          return;
        }
        IntraClassCalls calls = null;
        for (JavaCodeUnit listener : item.getCodeUnits()) {
          if (AnnotationRoles.listenerEntry(listener, roles.eventListener()) == null) {
            continue;
          }
          if (calls == null) {
            calls = new IntraClassCalls(item);
          }
          java.util.Set<String> ports = new java.util.TreeSet<>();
          for (JavaCodeUnit unit : calls.reachableFrom(listener)) {
            for (JavaMethodCall call : unit.getMethodCallsFromSelf()) {
              JavaClass owner = call.getTargetOwner();
              if (markers.is("inputPort").test(owner)) {
                ports.add(owner.getSimpleName() + "." + call.getName());
              }
            }
          }
          if (ports.isEmpty()) {
            continue;
          }
          boolean afterCommit =
              AnnotationRoles.listenerEntry(listener, roles.transactionalEventListener()) != null;
          boolean ownTransaction =
              AnnotationRoles.isMetaAnnotatedWithAny(listener, roles.transactional())
                  || AnnotationRoles.isMetaAnnotatedWithAny(item, roles.transactional());
          if (!afterCommit || !ownTransaction) {
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getName()
                        + "."
                        + listener.getName()
                        + " drives "
                        + String.join(", ", ports)
                        + (afterCommit
                            ? " after the commit without a transaction of its own"
                            : " outside the phase after a successful commit - inside the"
                                + " transaction that raised the event, or whatever its outcome")));
          }
        }
      }
    };
  }

  /**
   * DCA-HEX-018. A controller is an incoming adapter: it lives where the incoming adapters live, or
   * every rule that selects incoming adapters passes it by.
   */
  public DcaRule controllersResideInIncomingAdapters() {
    List<String> webControllers = layout.frameworkAnnotations().webController();
    List<String> restControllers = layout.frameworkAnnotations().restController();
    return DcaRule.of(
            "DCA-HEX-018",
            "Controllers must reside in an incoming adapter package",
            "A controller turns a request into a command or a query and is an incoming adapter."
                + " Outside the incoming adapter packages it is outside the selection of every rule"
                + " written for incoming adapters - the GET-handler rule, the controller naming"
                + " rules, the boundary rules - and passes them all",
            arch ->
                classes()
                    .that()
                    .areNotInterfaces()
                    .and(AnnotationRoles.annotatedWithAny(webControllers, restControllers))
                    .should(ArchConditions.be(arch.inIncomingAdapter()))
                    .allowEmptyShould(true))
        .experimental()
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are directly annotated"
                + " with one of the configured web-controller or REST-controller stereotypes. An"
                + " interface carrying the same annotation - a declarative REST client in an outgoing"
                + " adapter - is not selected, and a class is not selected for its name. With empty web-controller and REST-controller roles"
                + " nothing is selected and the rule passes.")
        .checking(
            "Each resides in <module>.adapter.incoming.. of some module root. Where in the incoming"
                + " adapters it sits - which protocol segment - is not checked.",
            "move the controller into the incoming adapter package of its module");
  }

  /**
   * DCA-HEX-019. Every implementation of an output port is an outgoing adapter. The composition
   * root wires an implementation to its port and holds none itself - except the bridge that hands a
   * port a second use case: it implements an output port and drives an input port, so it is wiring,
   * and in Java usually a lambda that is no class at all.
   */
  public DcaRule outputPortImplementationsResideInOutgoingAdapters() {
    return DcaRule.check(
            "DCA-HEX-019",
            "Output port implementations are placed in the outgoing adapters",
            "An output port is what the application needs from outside; whatever implements it is an"
                + " outgoing adapter - the implemented port says so. An implementation placed in the"
                + " infrastructure package, the application, the domain or in no layer at all is"
                + " outside the selection of every rule written for outgoing adapters, and"
                + " infrastructure then holds behaviour instead of wiring alone",
            arch -> {
              CollectedViolations violations =
                  CollectedViolations.withHeader(
                      "Output port implementations are placed in the outgoing adapters\nbecause"
                          + " whatever implements an output port is an outgoing adapter");
              var outputPort = arch.layout().markers().is("outputPort");
              var inputPort = arch.layout().markers().is("inputPort");
              for (var type : arch.classes()) {
                if (type.isInterface() || !outputPort.test(type)) {
                  continue;
                }
                var placement = arch.placement(type);
                if (placement.isEmpty()) {
                  violations.add(
                      type.getName()
                          + " implements an output port but sits in no layer - declare the layer"
                          + " of its package (DCA-LAY-007 lists such classes), or switch the"
                          + " hexagonal set off while the project has no layers");
                  continue;
                }
                if (placement.get().layer() == DcaLayout.Layer.OUTGOING_ADAPTER) {
                  continue;
                }
                boolean bridge =
                    placement.get().layer() == DcaLayout.Layer.INFRASTRUCTURE
                        && type.getDirectDependenciesFromSelf().stream()
                            .anyMatch(d -> inputPort.test(d.getTargetClass()));
                if (!bridge) {
                  violations.add(
                      type.getName()
                          + " implements an output port but is placed in the "
                          + placement.get().layer().role()
                          + " layer ("
                          + placement.get().source().name().toLowerCase(java.util.Locale.ROOT)
                          + " "
                          + placement.get().layerPackage()
                          + ")");
                }
              }
              violations.throwIfAny();
            })
        .experimental()
        .selecting(
            "Non-interface classes anywhere under scan assignable to the output-port role -"
                + " repositories, stores, event publishers and every other output port alike,"
                + " abstract base classes and anonymous classes included. A lambda or a method"
                + " reference is no class under scan and is not selected. Not selected: a class in"
                + " an infrastructure package that depends on a type assignable to the input-port"
                + " role - the bridge that hands an output port a second use case, which is wiring.")
        .checking(
            "Each is placed in the outgoing adapter layer - by default <module>.adapter.outgoing..,"
                + " or wherever the layout's outgoing-adapter marker places it; a class the layout"
                + " places in no layer is reported as well. DCA-TAC-015 and"
                + " DCA-TAC-020 hold repositories and stores the same way in the tactical set.",
            "move the implementation into the outgoing adapter layer of its module; the"
                + " configuration in infrastructure only wires it to its port");
  }

  public DcaRule adaptersPublishNoEnumMemberName() {
    return DcaRule.informational(
            "DCA-HEX-016",
            "Diagnostic: adapters that publish an enum by its member name",
            "A value an adapter publishes from an enum is spelled explicitly - a code per value on"
                + " the enum or a mapping in the adapter - never derived from the member's name."
                + " Renaming a member is a refactoring inside the code and must not change what a"
                + " client reads",
            arch -> {
              var inner = arch.inDomain().or(arch.inApplication());
              java.util.Set<String> observed = new java.util.TreeSet<>();
              for (JavaClass adapter : arch.classes()) {
                if (!arch.inAdapter().test(adapter)) {
                  continue;
                }
                for (JavaMethodCall call : adapter.getMethodCallsFromSelf()) {
                  JavaClass owner = call.getTargetOwner();
                  // the compiler's lambda deserialisation repeats the calls of a lambda's body
                  if (call.getOrigin().getName().equals("$deserializeLambda$")) {
                    continue;
                  }
                  if ((call.getName().equals("name") || call.getName().equals("toString"))
                      && call.getTarget().getRawParameterTypes().isEmpty()
                      && owner.isEnum()
                      && inner.test(owner)) {
                    observed.add(
                        call.getOrigin().getFullName()
                            + " spells "
                            + owner.getSimpleName()
                            + " by its member name ("
                            + call.getName()
                            + "()) - give the enum a code per value, or map it in the adapter");
                  }
                }
              }
              return new ArrayList<>(observed);
            })
        .experimental()
        .selecting(
            "Classes in <module>.adapter.. of every module root, incoming and outgoing, and their"
                + " method calls whose target is an enum declared in <module>.domain.. or"
                + " <module>.application..")
        .checking(
            "Informational diagnostic only: lists every call of name() or toString() on such an"
                + " enum - inside a string concatenation as well - and never fails. The enum value"
                + " itself concatenated (\"text\" + value) or passed to a formatting or"
                + " string-building call reaches the platform as an Object - the compiler erases"
                + " its type from the call - so it is not visible and is not listed, nor is a"
                + " serializer that writes the name on its own; whether the value leaves the"
                + " process is not established either.");
  }
}
