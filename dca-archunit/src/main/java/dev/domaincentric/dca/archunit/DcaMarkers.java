package dev.domaincentric.dca.archunit;

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
 * <p>Every role holds exactly one type name, matched by assignability: a class or interface the
 * project's own type extends or implements, directly or through an intermediate type. Two
 * vocabularies at once — the library's own marker on one aggregate and another's on the next — are
 * outside this: a role names one type, so a migration points the role at one of them and the rules
 * select what carries it.
 *
 * <p>What the roles do <em>not</em> cover are the strategic annotations ({@code @BoundedContext},
 * {@code @Upstream}, {@code @Partnership} and their siblings). The rules do not only select on
 * them, they read their members — the context a relationship names, the dependencies a module
 * allows — and a name alone does not carry members. Those annotations stay the library's own.
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
 * @param domainException the base type of a domain failure
 * @param useCaseException the base type of a use-case failure
 * @param transactionBoundary the explicit transaction boundary of the application layer
 * @param inputPort the base type of every incoming (driving) port
 * @param useCase the input port that takes a command or query and answers with a result
 * @param outputPort the base type of every outgoing (driven) port
 * @param repository the outgoing port that loads and stores an aggregate
 * @param store the outgoing port that holds data no aggregate owns
 * @param domainEventPublisher the outgoing port that publishes an aggregate's domain events
 * @param integrationEventPublisher the outgoing port that publishes an integration event — read by
 *     {@code DCA-USE-013}, which separates the output ports that live inside the transaction from
 *     those that may leave the process. The .NET twin carries the role without a caller, because
 *     that rule is not applicable there
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
    String domainException,
    String useCaseException,
    String transactionBoundary,
    String inputPort,
    String useCase,
    String outputPort,
    String repository,
    String store,
    String domainEventPublisher,
    String integrationEventPublisher) {

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
        DCA_BUILDING_BLOCKS + ".ddd.tactical.DomainException",
        DCA_BUILDING_BLOCKS + ".application.UseCaseException",
        DCA_BUILDING_BLOCKS + ".application.TransactionBoundary",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.in.InputPort",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.in.UseCase",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.OutputPort",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.Repository",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.Store",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.DomainEventPublisher",
        DCA_BUILDING_BLOCKS + ".hexagonal.port.out.IntegrationEventPublisher");
  }

  /** The same vocabulary under a different name, which the test report shows. */
  public DcaMarkers named(String vocabulary) {
    return from(vocabulary, roles());
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
    return from(name, next);
  }

  private static DcaMarkers from(String name, Map<String, String> roles) {
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
        roles.get("domainException"),
        roles.get("useCaseException"),
        roles.get("transactionBoundary"),
        roles.get("inputPort"),
        roles.get("useCase"),
        roles.get("outputPort"),
        roles.get("repository"),
        roles.get("store"),
        roles.get("domainEventPublisher"),
        roles.get("integrationEventPublisher"));
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
    return roles().equals(dca().roles());
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
    return roles;
  }

  @Override
  public String toString() {
    return name;
  }
}
