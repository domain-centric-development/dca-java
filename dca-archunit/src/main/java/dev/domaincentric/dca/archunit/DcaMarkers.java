package dev.domaincentric.dca.archunit;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The building-block types the DCA rules select on, by <em>role</em>, each named by its fully
 * qualified name. A rule asks the layout for the role it needs — "the aggregate-root marker", "the
 * repository port" — and never for a specific type, so the same rules govern a code base that
 * carries its own markers or another library's.
 *
 * <p>The default is {@link #dca()}: the markers of {@code dca-building-blocks}. A project that
 * already has an established vocabulary keeps it and points the roles at its own types:
 *
 * <pre>{@code
 * DcaLayout.forBasePackage("com.acme.billing")
 *     .withMarkers(
 *         DcaMarkers.dca()
 *             .named("jmolecules")
 *             .withAggregateRoot("org.jmolecules.ddd.types.AggregateRoot")
 *             .withEntity("org.jmolecules.ddd.types.Entity")
 *             .withValue("org.jmolecules.ddd.types.ValueObject")
 *             .withId("org.jmolecules.ddd.types.Identifier")
 *             .withRepository("org.jmolecules.ddd.types.Repository"));
 * }</pre>
 *
 * <p>Every role holds one type name, matched by assignability: a class or interface the project's
 * own type extends or implements, directly or through an intermediate type. Where a project marks
 * its building blocks with annotations instead, the role takes a {@link DcaMarker} — {@code
 * withValue(DcaMarker.ofAnnotation("com.acme.ValueObject"))}, or {@code anyOf} a type and an
 * annotation while a migration has both; the annotation counts on the class and on its supertypes,
 * directly or as a meta-annotation. A package never names a role. Two roles are recognised by name
 * by default: {@code command} and {@code query}, the input of a use case, {@code
 * ofSuffix("Command")} and {@code ofSuffix("Query")} — {@link #withCommand(DcaMarker)} and {@link
 * #withQuery(DcaMarker)} point them at a supertype or an annotation instead. A rule that reads a
 * role's generic arguments or method names (a repository's aggregate, {@code save}) reads them from
 * the role's type.
 *
 * <p><b>The port roles need a common root.</b> {@code withOutputPort} must be set whenever any
 * other port role is: {@code DCA-HEX-009} measures every output port of the project against it, so
 * leaving it at the library's own type while {@code repository} points elsewhere reports every port
 * of the project's vocabulary. A vocabulary that has no common port supertype cannot express that
 * here - switch {@code DCA-HEX-009} off rather than pointing the role at an unrelated type.
 *
 * <p><b>Three roles name annotations</b>, not supertypes: {@code boundedContext} and {@code
 * sharedKernel} on a {@code package-info}, {@code integrationEventType} on an integration event.
 * They are matched by name on the imported bytecode, never by loading a class, and the members the
 * rules read are read by name: a project's own {@code @BoundedContext(name, description)} serves
 * like the library's. A member the rules need and the annotation lacks is reported by name.
 *
 * <p>What the roles do <em>not</em> cover are the relationship annotations ({@code @Upstream},
 * {@code @ExternalUpstream}, {@code @Partnership}, {@code @OpenHostService}). The rules read their
 * nested values and enum constants — the channels a relationship consumes, its translation and its
 * status — and those stay the library's own.
 *
 * <p><b>A role names a type, never a method.</b> Four method names are fixed, and pointing a role
 * at your own type does not rename them: {@code save} and {@code deleteById} / {@code delete} on
 * the repository role, {@code publishAndClearEvents} on the domain-event-publisher role, and {@code
 * registerEvent} on the aggregate root. The rules that reason about a unit of work — {@code
 * DCA-USE-009}, {@code DCA-USE-012}, {@code DCA-TAC-021} and the event-free-aggregate proof behind
 * {@code DCA-USE-009} — look for calls by those names. A vocabulary whose repository writes through
 * {@code store(...)} or {@code persist(...)} is selected correctly and then found to save nothing,
 * so those rules pass over it. Keep the four names, or switch the affected ids off with a recorded
 * reason.
 *
 * @param name the vocabulary's name ({@code dca}, or whatever {@link #named(String)} set). {@code
 *     DcaArchitectureTest} prints it as a passing diagnostic case together with the roles that
 *     differ from the default, so a reader knows which types the rules resolved; {@code
 *     DcaLayout.markersReport()} is the same line without JUnit
 * @param aggregateRoot the marker of an aggregate root
 * @param entity the marker of an entity
 * @param value the marker of a value object
 * @param id the marker of an identifier
 * @param domainEvent the marker of a domain event
 * @param integrationEvent the marker of an integration event
 * @param domainService the marker of a domain service
 * @param factory the marker of a factory
 * @param specification the marker of a specification — a predicate over a domain object, selected
 *     by {@code DCA-ADV-017} and {@code DCA-ADV-018} alongside the name suffix
 * @param domainException the base type of a domain failure
 * @param useCaseException the base type of a use-case failure
 * @param transactionBoundary the explicit transaction boundary of the application layer
 * @param inputPort the base type of every incoming (driving) port
 * @param useCase the input port that takes a command or query and answers with a result - read only
 *     by {@code DCA-NAM-001}; every other use-case rule selects through {@code inputPort} or the
 *     use-case suffix. A vocabulary with several use-case shapes (one that answers, one that
 *     answers nothing) points this role at their common supertype, which may be the input port
 * @param outputPort the base type of every outgoing (driven) port
 * @param repository the outgoing port that loads and stores an aggregate
 * @param store the outgoing port that holds data no aggregate owns
 * @param domainEventPublisher the outgoing port that publishes an aggregate's domain events
 * @param integrationEventPublisher the outgoing port that publishes an integration event — read by
 *     {@code DCA-USE-013}, which separates the output ports that live inside the transaction from
 *     those that may leave the process. The .NET twin carries the role without a caller, because
 *     that rule is not applicable there
 * @param boundedContext the annotation that declares a bounded context on a {@code package-info};
 *     its members {@code name} and {@code description} are read by name
 * @param sharedKernel the annotation that declares the shared kernel on a {@code package-info}
 * @param integrationEventType the annotation that gives an integration event its contract identity
 *     - {@code DCA-ADV-005} reads its presence
 * @param recognisedBy how a role is recognised where that is more than assignability to its type
 *     (WP-160): a building-block role by {@code ofType} and/or {@code ofAnnotation}, the {@code
 *     command} and {@code query} roles also by {@code ofSuffix} — their default — and the three
 *     declaration roles by {@code ofAnnotation}. A role absent from the map is its type, by
 *     assignability
 */
public record DcaMarkers(
    String name,
    String aggregateRoot,
    String entity,
    String value,
    String id,
    String domainEvent,
    String integrationEvent,
    String domainService,
    String factory,
    String specification,
    String domainException,
    String useCaseException,
    String transactionBoundary,
    String inputPort,
    String useCase,
    String outputPort,
    String repository,
    String store,
    String domainEventPublisher,
    String integrationEventPublisher,
    String boundedContext,
    String sharedKernel,
    String integrationEventType,
    Map<String, DcaMarker> recognisedBy) {

  /** The two roles that are recognised by name by default: a use case's input. */
  public static final List<String> NAMED_ROLES = List.of("command", "query");

  private static final Map<String, DcaMarker> DEFAULT_RECOGNITION =
      Map.of(
          "command", DcaMarker.ofSuffix("Command"),
          "query", DcaMarker.ofSuffix("Query"));

  private static final List<String> DECLARATION_ROLES =
      List.of("boundedContext", "sharedKernel", "integrationEventType");

  /** The vocabulary as before WP-160: every role its type, command and query by suffix. */
  public DcaMarkers(
      String name,
      String aggregateRoot,
      String entity,
      String value,
      String id,
      String domainEvent,
      String integrationEvent,
      String domainService,
      String factory,
      String specification,
      String domainException,
      String useCaseException,
      String transactionBoundary,
      String inputPort,
      String useCase,
      String outputPort,
      String repository,
      String store,
      String domainEventPublisher,
      String integrationEventPublisher,
      String boundedContext,
      String sharedKernel,
      String integrationEventType) {
    this(
        name,
        aggregateRoot,
        entity,
        value,
        id,
        domainEvent,
        integrationEvent,
        domainService,
        factory,
        specification,
        domainException,
        useCaseException,
        transactionBoundary,
        inputPort,
        useCase,
        outputPort,
        repository,
        store,
        domainEventPublisher,
        integrationEventPublisher,
        boundedContext,
        sharedKernel,
        integrationEventType,
        DEFAULT_RECOGNITION);
  }

  /** The package the default vocabulary lives in. */
  public static final String DCA_BUILDING_BLOCKS = "dev.domaincentric.dca.buildingblocks";

  public DcaMarkers {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    aggregateRoot = type(aggregateRoot, "aggregateRoot");
    entity = type(entity, "entity");
    value = type(value, "value");
    id = type(id, "id");
    domainEvent = type(domainEvent, "domainEvent");
    integrationEvent = type(integrationEvent, "integrationEvent");
    domainService = type(domainService, "domainService");
    factory = type(factory, "factory");
    specification = type(specification, "specification");
    domainException = type(domainException, "domainException");
    useCaseException = type(useCaseException, "useCaseException");
    transactionBoundary = type(transactionBoundary, "transactionBoundary");
    inputPort = type(inputPort, "inputPort");
    useCase = type(useCase, "useCase");
    outputPort = type(outputPort, "outputPort");
    repository = type(repository, "repository");
    store = type(store, "store");
    domainEventPublisher = type(domainEventPublisher, "domainEventPublisher");
    integrationEventPublisher = type(integrationEventPublisher, "integrationEventPublisher");
    boundedContext = type(boundedContext, "boundedContext");
    sharedKernel = type(sharedKernel, "sharedKernel");
    integrationEventType = type(integrationEventType, "integrationEventType");
    Map<String, DcaMarker> recognition = new LinkedHashMap<>(DEFAULT_RECOGNITION);
    if (recognisedBy != null) {
      recognition.putAll(recognisedBy);
    }
    recognisedBy = java.util.Collections.unmodifiableMap(recognition);
  }

  private static String type(String fqn, String role) {
    Objects.requireNonNull(fqn, role);
    if (fqn.isBlank()) {
      throw new IllegalArgumentException(
          role
              + " must name a type: a role selects by assignability, and an empty role would"
              + " silently select nothing");
    }
    return fqn;
  }

  /** The markers of {@code dca-building-blocks} — the default of {@link DcaLayout}. */
  public static DcaMarkers dca() {
    return new DcaMarkers(
        "dca",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.AggregateRoot",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.Entity",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.Value",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.Id",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.DomainEvent",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.IntegrationEvent",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.DomainService",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.Factory",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.Specification",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.DomainException",
        DCA_BUILDING_BLOCKS + ".application.UseCaseException",
        DCA_BUILDING_BLOCKS + ".application.TransactionBoundary",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.in.InputPort",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.in.UseCase",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.OutputPort",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.Repository",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.Store",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.DomainEventPublisher",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.IntegrationEventPublisher",
        DCA_BUILDING_BLOCKS + ".ddd.strategic.BoundedContext",
        DCA_BUILDING_BLOCKS + ".ddd.strategic.relationships.SharedKernel",
        DCA_BUILDING_BLOCKS + ".ddd.tactical.IntegrationEventType");
  }

  /** The same vocabulary under a different name, which the test report shows. */
  public DcaMarkers named(String vocabulary) {
    return from(vocabulary, roles(), recognisedBy);
  }

  /**
   * The same vocabulary with one role pointed at another type. The role is named as {@link
   * #roles()} names it ({@code "aggregateRoot"}, {@code "repository"}, …); an unknown name fails
   * rather than silently configuring nothing, and so does a blank type name.
   */
  public DcaMarkers withRole(String role, String fqn) {
    Map<String, String> next = new LinkedHashMap<>(roles());
    if (!next.containsKey(role)) {
      throw new IllegalArgumentException(
          "No marker role named '" + role + "' - known: " + next.keySet());
    }
    next.put(role, type(fqn, role));
    Map<String, DcaMarker> recognition = new LinkedHashMap<>(recognisedBy);
    recognition.remove(role);
    return from(name, next, recognition);
  }

  /**
   * The same vocabulary with one role recognised by the given marker (WP-160). A building-block
   * role takes {@link DcaMarker#ofType(String)}, {@link DcaMarker#ofAnnotation(String)} on the
   * type, or both; {@code command} and {@code query} also {@link DcaMarker#ofSuffix(String)}; the
   * three declaration roles {@code ofAnnotation} only. A package never names a role — it says where
   * a type sits, not what it is. At most one {@code ofType}: it is the type the role names, and the
   * rules that read a role's generic arguments or methods read them from it. A role given without
   * {@code ofType} keeps its type name for those readings but is not recognised by assignability to
   * it.
   */
  public DcaMarkers withRole(String role, DcaMarker marker) {
    Objects.requireNonNull(marker, role);
    boolean named = NAMED_ROLES.contains(role);
    if (!named && !roles().containsKey(role)) {
      throw new IllegalArgumentException(
          "No marker role named '" + role + "' - known: " + roles().keySet() + ", " + NAMED_ROLES);
    }
    if (DECLARATION_ROLES.contains(role)) {
      marker.requireKinds("The " + role + " role", DcaMarker.Kind.ANNOTATION);
    } else if (named) {
      marker.requireKinds(
          "The " + role + " role",
          DcaMarker.Kind.TYPE,
          DcaMarker.Kind.ANNOTATION,
          DcaMarker.Kind.SUFFIX);
    } else {
      marker.requireKinds("The " + role + " role", DcaMarker.Kind.TYPE, DcaMarker.Kind.ANNOTATION);
    }
    List<String> types = marker.valuesOf(DcaMarker.Kind.TYPE);
    if (types.size() > 1) {
      throw new IllegalArgumentException(
          "The " + role + " role names one type, was " + types + " - name their common supertype");
    }
    Map<String, String> next = new LinkedHashMap<>(roles());
    if (!named && !types.isEmpty()) {
      next.put(role, types.get(0));
    }
    if (DECLARATION_ROLES.contains(role)) {
      next.put(role, marker.valuesOf(DcaMarker.Kind.ANNOTATION).get(0));
    }
    Map<String, DcaMarker> recognition = new LinkedHashMap<>(recognisedBy);
    boolean plainType =
        !named && marker.kind() == DcaMarker.Kind.TYPE || DECLARATION_ROLES.contains(role);
    if (plainType) {
      recognition.remove(role);
    } else {
      recognition.put(role, marker);
    }
    return from(name, next, recognition);
  }

  /** How a role is recognised: its marker, or {@code ofType} of the type it names. */
  public DcaMarker marker(String role) {
    DcaMarker marker = recognisedBy.get(role);
    if (marker != null) {
      return marker;
    }
    String type = roles().get(role);
    if (type == null) {
      throw new IllegalArgumentException("No marker role named '" + role + "'");
    }
    return DECLARATION_ROLES.contains(role) ? DcaMarker.ofAnnotation(type) : DcaMarker.ofType(type);
  }

  /**
   * The classes a role recognises: assignable to its type, carrying its annotation — on the class
   * or on one of its supertypes, directly or as a meta-annotation — or named with its suffix, as
   * its marker says.
   */
  public DescribedPredicate<JavaClass> is(String role) {
    DcaMarker marker = marker(role);
    return DescribedPredicate.describe(
        "are " + role + " (" + marker + ")", c -> matches(marker, c));
  }

  /** Whether the class is one the marker recognises — {@link #is(String)} for a single class. */
  public static boolean matches(DcaMarker marker, JavaClass javaClass) {
    for (DcaMarker alternative : marker.alternatives()) {
      boolean hit =
          switch (alternative.kind()) {
            case TYPE -> javaClass.isAssignableTo(alternative.value());
            case ANNOTATION -> annotatedInHierarchy(javaClass, alternative.value());
            case SUFFIX -> javaClass.getSimpleName().endsWith(alternative.value());
            case PACKAGE, ANY_OF -> false;
          };
      if (hit) {
        return true;
      }
    }
    return false;
  }

  private static boolean annotatedInHierarchy(JavaClass javaClass, String annotation) {
    if (javaClass.isAnnotatedWith(annotation) || javaClass.isMetaAnnotatedWith(annotation)) {
      return true;
    }
    for (JavaClass type : javaClass.getAllRawSuperclasses()) {
      if (type.isAnnotatedWith(annotation) || type.isMetaAnnotatedWith(annotation)) {
        return true;
      }
    }
    for (JavaClass type : javaClass.getAllRawInterfaces()) {
      if (type.isAnnotatedWith(annotation) || type.isMetaAnnotatedWith(annotation)) {
        return true;
      }
    }
    return false;
  }

  /** The roles recognised otherwise than by plain assignability, for the report. */
  public Map<String, DcaMarker> recognisedOtherwise() {
    Map<String, DcaMarker> changed = new LinkedHashMap<>();
    recognisedBy.forEach(
        (role, marker) -> {
          if (!marker.equals(DEFAULT_RECOGNITION.get(role))) {
            changed.put(role, marker);
          }
        });
    return changed;
  }

  public DcaMarkers withCommand(DcaMarker marker) {
    return withRole("command", marker);
  }

  public DcaMarkers withQuery(DcaMarker marker) {
    return withRole("query", marker);
  }

  private static DcaMarkers from(
      String name, Map<String, String> roles, Map<String, DcaMarker> recognisedBy) {
    return new DcaMarkers(
        name,
        roles.get("aggregateRoot"),
        roles.get("entity"),
        roles.get("value"),
        roles.get("id"),
        roles.get("domainEvent"),
        roles.get("integrationEvent"),
        roles.get("domainService"),
        roles.get("factory"),
        roles.get("specification"),
        roles.get("domainException"),
        roles.get("useCaseException"),
        roles.get("transactionBoundary"),
        roles.get("inputPort"),
        roles.get("useCase"),
        roles.get("outputPort"),
        roles.get("repository"),
        roles.get("store"),
        roles.get("domainEventPublisher"),
        roles.get("integrationEventPublisher"),
        roles.get("boundedContext"),
        roles.get("sharedKernel"),
        roles.get("integrationEventType"),
        recognisedBy);
  }

  public DcaMarkers withAggregateRoot(String fqn) {
    return withRole("aggregateRoot", fqn);
  }

  public DcaMarkers withEntity(String fqn) {
    return withRole("entity", fqn);
  }

  public DcaMarkers withValue(String fqn) {
    return withRole("value", fqn);
  }

  public DcaMarkers withId(String fqn) {
    return withRole("id", fqn);
  }

  public DcaMarkers withDomainEvent(String fqn) {
    return withRole("domainEvent", fqn);
  }

  public DcaMarkers withIntegrationEvent(String fqn) {
    return withRole("integrationEvent", fqn);
  }

  public DcaMarkers withDomainService(String fqn) {
    return withRole("domainService", fqn);
  }

  public DcaMarkers withFactory(String fqn) {
    return withRole("factory", fqn);
  }

  public DcaMarkers withSpecification(String fqn) {
    return withRole("specification", fqn);
  }

  public DcaMarkers withDomainException(String fqn) {
    return withRole("domainException", fqn);
  }

  public DcaMarkers withUseCaseException(String fqn) {
    return withRole("useCaseException", fqn);
  }

  public DcaMarkers withTransactionBoundary(String fqn) {
    return withRole("transactionBoundary", fqn);
  }

  public DcaMarkers withInputPort(String fqn) {
    return withRole("inputPort", fqn);
  }

  public DcaMarkers withUseCase(String fqn) {
    return withRole("useCase", fqn);
  }

  public DcaMarkers withOutputPort(String fqn) {
    return withRole("outputPort", fqn);
  }

  public DcaMarkers withRepository(String fqn) {
    return withRole("repository", fqn);
  }

  public DcaMarkers withStore(String fqn) {
    return withRole("store", fqn);
  }

  public DcaMarkers withDomainEventPublisher(String fqn) {
    return withRole("domainEventPublisher", fqn);
  }

  public DcaMarkers withIntegrationEventPublisher(String fqn) {
    return withRole("integrationEventPublisher", fqn);
  }

  /**
   * The annotation that declares a bounded context on a {@code package-info}. The rules read its
   * members {@code name} and {@code description} by name, so the project's own annotation must
   * declare {@code name}; {@code description} may be absent.
   */
  public DcaMarkers withBoundedContext(String fqn) {
    return withRole("boundedContext", fqn);
  }

  /**
   * The annotation that declares the shared kernel on a {@code package-info}. Only its presence is
   * read; a member {@code description} is read when the annotation has one.
   */
  public DcaMarkers withSharedKernel(String fqn) {
    return withRole("sharedKernel", fqn);
  }

  /**
   * The annotation that gives an integration event its contract identity. {@code DCA-ADV-005} reads
   * only its presence on the event class.
   */
  public DcaMarkers withIntegrationEventType(String fqn) {
    return withRole("integrationEventType", fqn);
  }

  public DcaMarkers withAggregateRoot(DcaMarker marker) {
    return withRole("aggregateRoot", marker);
  }

  public DcaMarkers withEntity(DcaMarker marker) {
    return withRole("entity", marker);
  }

  public DcaMarkers withValue(DcaMarker marker) {
    return withRole("value", marker);
  }

  public DcaMarkers withId(DcaMarker marker) {
    return withRole("id", marker);
  }

  public DcaMarkers withDomainEvent(DcaMarker marker) {
    return withRole("domainEvent", marker);
  }

  public DcaMarkers withIntegrationEvent(DcaMarker marker) {
    return withRole("integrationEvent", marker);
  }

  public DcaMarkers withDomainService(DcaMarker marker) {
    return withRole("domainService", marker);
  }

  public DcaMarkers withFactory(DcaMarker marker) {
    return withRole("factory", marker);
  }

  public DcaMarkers withSpecification(DcaMarker marker) {
    return withRole("specification", marker);
  }

  public DcaMarkers withDomainException(DcaMarker marker) {
    return withRole("domainException", marker);
  }

  public DcaMarkers withUseCaseException(DcaMarker marker) {
    return withRole("useCaseException", marker);
  }

  public DcaMarkers withTransactionBoundary(DcaMarker marker) {
    return withRole("transactionBoundary", marker);
  }

  public DcaMarkers withInputPort(DcaMarker marker) {
    return withRole("inputPort", marker);
  }

  public DcaMarkers withUseCase(DcaMarker marker) {
    return withRole("useCase", marker);
  }

  public DcaMarkers withOutputPort(DcaMarker marker) {
    return withRole("outputPort", marker);
  }

  public DcaMarkers withRepository(DcaMarker marker) {
    return withRole("repository", marker);
  }

  public DcaMarkers withStore(DcaMarker marker) {
    return withRole("store", marker);
  }

  public DcaMarkers withDomainEventPublisher(DcaMarker marker) {
    return withRole("domainEventPublisher", marker);
  }

  public DcaMarkers withIntegrationEventPublisher(DcaMarker marker) {
    return withRole("integrationEventPublisher", marker);
  }

  public DcaMarkers withBoundedContext(DcaMarker marker) {
    return withRole("boundedContext", marker);
  }

  public DcaMarkers withSharedKernel(DcaMarker marker) {
    return withRole("sharedKernel", marker);
  }

  public DcaMarkers withIntegrationEventType(DcaMarker marker) {
    return withRole("integrationEventType", marker);
  }

  /**
   * The packages the role types themselves live in, as ArchUnit patterns ({@code pkg..}) — the
   * vocabulary's own code, which is never the project's. Rules that ask "is this the project's own
   * type or the vocabulary's" use this instead of a hard-wired library package: with the default
   * roles it yields the building blocks' packages, with a project's own markers the packages those
   * markers live in. Deriving it from the roles is what keeps {@code DCA-ERR-002} from reporting a
   * foreign base class as an exception outside its layer.
   */
  public List<String> declaringPackagePatterns() {
    return packagePatternsOf(roles().values());
  }

  /**
   * The roles whose types a domain class may legitimately depend on: the tactical vocabulary and
   * the outgoing ports. The application-layer roles ({@code useCaseException}, {@code
   * transactionBoundary}) and the incoming ports ({@code inputPort}, {@code useCase}) are
   * deliberately absent — a domain class that reaches for one of them is the violation {@code
   * DCA-ONI-002} exists to report.
   */
  public static final List<String> DOMAIN_FACING_ROLES =
      List.of(
          "aggregateRoot",
          "entity",
          "value",
          "id",
          "domainEvent",
          "integrationEvent",
          "domainService",
          "factory",
          "specification",
          "domainException",
          "outputPort",
          "repository",
          "store",
          "domainEventPublisher",
          "integrationEventPublisher");

  /**
   * The packages of the named roles, as ArchUnit patterns.
   *
   * <p>This is what a rule asks for when it needs "the packages the vocabulary declares these types
   * in" rather than a hard-wired library package. With the default roles and {@link
   * #DOMAIN_FACING_ROLES} it yields the building blocks' tactical and outgoing-port packages —
   * exactly the two that used to be written into {@code DCA-ONI-002} by hand — and with a project's
   * own markers it yields the packages those markers live in, so the project's vocabulary is not
   * reported as a foreign library inside its own domain.
   *
   * @param roleNames role names as {@link #roles()} keys them; an unknown name is ignored
   */
  public List<String> declaringPackagePatternsOf(Collection<String> roleNames) {
    Map<String, String> roles = roles();
    return packagePatternsOf(roleNames.stream().map(roles::get).filter(Objects::nonNull).toList());
  }

  private static List<String> packagePatternsOf(Collection<String> fullyQualifiedNames) {
    return fullyQualifiedNames.stream()
        .map(fqn -> fqn.substring(0, Math.max(fqn.lastIndexOf('.'), 0)))
        .filter(pkg -> !pkg.isEmpty())
        .distinct()
        .sorted()
        .map(pkg -> pkg + "..")
        .toList();
  }

  /** Whether the named package is one the vocabulary's own types live in, or one below it. */
  public boolean declaresTypesIn(String packageName) {
    return roles().values().stream()
        .map(fqn -> fqn.substring(0, Math.max(fqn.lastIndexOf('.'), 0)))
        .filter(pkg -> !pkg.isEmpty())
        .anyMatch(pkg -> packageName.equals(pkg) || packageName.startsWith(pkg + "."));
  }

  /** Whether every role still names the type of {@link #dca()}. */
  public boolean isDefault() {
    return roles().equals(dca().roles()) && recognisedOtherwise().isEmpty();
  }

  /** The roles by name, in declaration order — what the report prints when they are not default. */
  public Map<String, String> roles() {
    Map<String, String> roles = new LinkedHashMap<>();
    roles.put("aggregateRoot", aggregateRoot);
    roles.put("entity", entity);
    roles.put("value", value);
    roles.put("id", id);
    roles.put("domainEvent", domainEvent);
    roles.put("integrationEvent", integrationEvent);
    roles.put("domainService", domainService);
    roles.put("factory", factory);
    roles.put("specification", specification);
    roles.put("domainException", domainException);
    roles.put("useCaseException", useCaseException);
    roles.put("transactionBoundary", transactionBoundary);
    roles.put("inputPort", inputPort);
    roles.put("useCase", useCase);
    roles.put("outputPort", outputPort);
    roles.put("repository", repository);
    roles.put("store", store);
    roles.put("domainEventPublisher", domainEventPublisher);
    roles.put("integrationEventPublisher", integrationEventPublisher);
    roles.put("boundedContext", boundedContext);
    roles.put("sharedKernel", sharedKernel);
    roles.put("integrationEventType", integrationEventType);
    return roles;
  }

  @Override
  public String toString() {
    return name;
  }
}
