package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Entity;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Factory;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * DDD tactical patterns (building blocks): Aggregate Roots, Entities, Value Objects, Repositories,
 * Stores and enriched read models.
 *
 * <p>References: Evans, <i>Domain-Driven Design</i> (2003); Vernon, <i>Implementing DDD</i> (2013)
 * — especially the four Rules of Aggregate Design; Millett/Tune, <i>Patterns, Principles, and
 * Practices of DDD</i> (2015).
 */
public final class TacticalPatternRules implements DcaRuleSet {

  private static final String REPOSITORY_SUFFIX = "Repository";
  private static final String STORE_SUFFIX = "Store";
  private static final Set<String> REPOSITORY_METHOD_NAMES =
      Set.of("findById", "save", "deleteById", "delete");

  private final DcaLayout layout;
  private final List<DcaRule> rules;

  public TacticalPatternRules(DcaLayout layout) {
    this.layout = Objects.requireNonNull(layout);
    this.rules =
        List.of(
            aggregateRootsImplementMarker(layout),
            aggregateRootsHoldNoOutputPorts(),
            aggregateRootsReferenceOtherAggregatesById(),
            entitiesHaveIdField(),
            entitiesHaveNoPublicConstructors(),
            domainModelHasNoPublicSetters(),
            entitiesReferenceAggregatesById(),
            valueObjectsContainNoEntities(),
            valueObjectClassesAreFinal(layout),
            valueObjectFieldsAreFinal(),
            valueObjectsHaveNoSetters(),
            valueObjectsAreRecordsOrHaveAttributeEquality(),
            repositoryInterfacesExtendMarker(layout),
            repositoryInterfacesResideInSharedOutputPorts(layout),
            repositoryImplementationsResideInOutgoingAdapters(layout),
            repositoriesOnlyForAggregateRoots(),
            repositoriesReturnNoNonRootEntities(),
            storeInterfacesExtendStoreMarker(),
            storeInterfacesResideInSharedOutputPorts(layout),
            storeImplementationsResideInOutgoingAdapters(layout),
            storeInterfacesHaveNoRepositorySemantics(),
            enrichedModelsAreValueRecords(layout));
  }

  @Override
  public String name() {
    return "tactical";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  /** The layout this rule set was built for. */
  public DcaLayout layout() {
    return layout;
  }

  // ---------------------------------------------------------------------------------------------
  // Aggregate Root pattern
  // ---------------------------------------------------------------------------------------------

  public static DcaRule aggregateRootsImplementMarker(DcaLayout layout) {
    return DcaRule.of(
        "DCA-TAC-001",
        "Aggregate Roots must implement AggregateRoot<T, ID>",
        "Classes named *AggregateRoot must implement AggregateRoot interface (DDD pattern)",
        arch ->
            classes()
                .that()
                .resideInAnyPackage(arch.allDomainModelPatterns())
                .and()
                .haveSimpleNameEndingWith("AggregateRoot")
                .and()
                .areNotInterfaces()
                .and()
                .doNotHaveSimpleName("AggregateRoot")
                .should()
                .implement(AggregateRoot.class)
                .allowEmptyShould(true));
  }

  public static DcaRule aggregateRootsHoldNoOutputPorts() {
    return DcaRule.check(
        "DCA-TAC-002",
        "Aggregate Roots must not hold references to Repositories or other Output Ports",
        "Aggregates are persistence-ignorant: repositories and services are passed as method"
            + " parameters by the use case, never injected as fields",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass aggregate : concreteClassesAssignableTo(arch, AggregateRoot.class)) {
            for (JavaField field : aggregate.getAllFields()) {
              JavaClass fieldType = field.getRawType();
              if (fieldType.isAssignableTo(Repository.class)
                  || fieldType.isAssignableTo(OutputPort.class)) {
                violations.add(
                    aggregate.getName()
                        + " has field '"
                        + field.getName()
                        + "' of type "
                        + fieldType.getName()
                        + " which is a repository/output port");
              }
            }
          }
          fail(
              "Aggregates must not have injected repositories or output ports - pass dependencies"
                  + " as method parameters.",
              violations);
        });
  }

  public static DcaRule aggregateRootsReferenceOtherAggregatesById() {
    return DcaRule.check(
        "DCA-TAC-003",
        "Aggregate Roots must not have fields with other Aggregate Root types",
        "Vernon's Aggregate Design Rule #2: reference other Aggregates by identity to keep"
            + " aggregate boundaries and transactional consistency intact",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass aggregate : concreteClassesAssignableTo(arch, AggregateRoot.class)) {
            for (JavaField field : TypeInspection.instanceFields(aggregate)) {
              for (JavaClass involved : TypeInspection.involvedTypes(field, aggregate)) {
                if (isConcreteAggregateRoot(involved)
                    && !isSelfReference(aggregate, field, involved)) {
                  violations.add(
                      fieldDescription(aggregate, field, involved)
                          + " which is another aggregate root");
                }
              }
            }
          }
          fail(
              "Aggregates must reference other aggregates by ID only (Vernon's Rule #2).",
              violations);
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Entity pattern
  // ---------------------------------------------------------------------------------------------

  public static DcaRule entitiesHaveIdField() {
    return DcaRule.check(
        "DCA-TAC-004",
        "Entities must have an ID field",
        "An Entity is defined by its identity, which is a value object implementing the Id marker",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass entity : concreteClassesAssignableTo(arch, Entity.class)) {
            if (entity.getModifiers().contains(JavaModifier.ABSTRACT)) {
              continue;
            }
            boolean hasIdField =
                entity.getAllFields().stream()
                    .anyMatch(f -> f.getRawType().isAssignableTo(Id.class));
            if (!hasIdField) {
              violations.add(
                  entity.getName()
                      + " has no field whose type implements "
                      + Id.class.getSimpleName());
            }
          }
          fail(
              "Entities must have an identity field typed as an Id value object (DDD pattern).",
              violations);
        });
  }

  public static DcaRule entitiesHaveNoPublicConstructors() {
    return DcaRule.check(
        "DCA-TAC-005",
        "Entities must not be instantiated directly from outside the aggregate",
        "Entities are created through their aggregate root so that the root can enforce its"
            + " invariants",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass entity : nonRootEntities(arch)) {
            if (entity.isRecord()) {
              continue;
            }
            entity.getConstructors().stream()
                .filter(c -> c.getModifiers().contains(JavaModifier.PUBLIC))
                .forEach(
                    c ->
                        violations.add(
                            entity.getName()
                                + " has public constructor - should be package-private or"
                                + " protected"));
          }
          fail(
              "Entities should not have public constructors (access only through aggregate root).\n"
                  + "Note: Records are excluded from this rule.",
              violations);
        });
  }

  public static DcaRule domainModelHasNoPublicSetters() {
    return DcaRule.check(
        "DCA-TAC-006",
        "Domain model classes must not have public setter methods",
        "Behavior-rich domain models change state through intention-revealing methods from the"
            + " ubiquitous language, never through public setters",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass domainClass : concreteClassesAssignableTo(arch, Entity.class)) {
            for (JavaMethod method : domainClass.getAllMethods()) {
              if (isSetter(method) && method.getModifiers().contains(JavaModifier.PUBLIC)) {
                violations.add(
                    domainClass.getName() + " has public setter '" + method.getName() + "'");
              }
            }
          }
          fail(
              "Domain model classes must not expose public setters - use intention-revealing"
                  + " methods from the ubiquitous language.",
              violations);
        });
  }

  public static DcaRule entitiesReferenceAggregatesById() {
    return DcaRule.check(
        "DCA-TAC-007",
        "Entities must not have fields with Aggregate Root types",
        "An entity inside an aggregate references other aggregates by identity only, otherwise the"
            + " aggregate boundary leaks",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass entity : nonRootEntities(arch)) {
            for (JavaField field : TypeInspection.instanceFields(entity)) {
              for (JavaClass involved : TypeInspection.involvedTypes(field, entity)) {
                if (isConcreteAggregateRoot(involved)) {
                  violations.add(
                      fieldDescription(entity, field, involved) + " which is an aggregate root");
                }
              }
            }
          }
          fail(
              "Entities must not contain references to aggregate roots (reference by ID only).",
              violations);
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Value Object pattern
  // ---------------------------------------------------------------------------------------------

  public static DcaRule valueObjectsContainNoEntities() {
    return DcaRule.check(
        "DCA-TAC-008",
        "Value Objects must not contain Aggregate Roots or Entities",
        "A Value Object is defined by its attributes; holding an object with identity would give"
            + " it a lifecycle it must not have",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass valueObject : concreteClassesAssignableTo(arch, Value.class)) {
            for (JavaField field : TypeInspection.instanceFields(valueObject)) {
              for (JavaClass involved : TypeInspection.involvedTypes(field, valueObject)) {
                if (isConcreteAggregateRoot(involved)) {
                  violations.add(
                      fieldDescription(valueObject, field, involved)
                          + " which is an aggregate root");
                }
                if (isConcreteNonRootEntity(involved)) {
                  violations.add(
                      fieldDescription(valueObject, field, involved) + " which is an entity");
                }
              }
            }
          }
          fail(
              "Value Objects must only contain other Value Objects or primitives (Vernon's DDD).",
              violations);
        });
  }

  public static DcaRule valueObjectClassesAreFinal(DcaLayout layout) {
    return DcaRule.of(
        "DCA-TAC-009",
        "Value Object classes should be final (immutability)",
        "Value objects should be immutable (final classes) - Vernon's DDD recommendation",
        arch ->
            classes()
                .that()
                .resideInAnyPackage(arch.allDomainModelPatterns())
                .and()
                .implement(Value.class)
                .and()
                .areNotInterfaces()
                .and()
                .areNotRecords()
                .should()
                .haveModifier(JavaModifier.FINAL)
                .allowEmptyShould(true));
  }

  public static DcaRule valueObjectFieldsAreFinal() {
    return DcaRule.check(
        "DCA-TAC-010",
        "Value Object fields must be final (deep immutability)",
        "Records have implicitly final fields and enums are immutable by design; a hand-written"
            + " value class must make every instance field final itself",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass valueObject : concreteClassesAssignableTo(arch, Value.class)) {
            if (valueObject.isRecord() || valueObject.isEnum()) {
              continue;
            }
            for (JavaField field : valueObject.getAllFields()) {
              Set<JavaModifier> modifiers = field.getModifiers();
              if (!modifiers.contains(JavaModifier.FINAL)
                  && !modifiers.contains(JavaModifier.STATIC)) {
                violations.add(
                    valueObject.getName() + " has non-final field '" + field.getName() + "'");
              }
            }
          }
          fail(
              "Value Object fields must be final for deep immutability (Vernon's DDD).",
              violations);
        });
  }

  public static DcaRule valueObjectsHaveNoSetters() {
    return DcaRule.check(
        "DCA-TAC-011",
        "Value Objects must not have setter methods",
        "Value Objects are immutable; state changes produce a new instance instead of mutating",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass valueObject : concreteClassesAssignableTo(arch, Value.class)) {
            for (JavaMethod method : valueObject.getAllMethods()) {
              if (isSetter(method)) {
                violations.add(
                    valueObject.getName() + " has setter method '" + method.getName() + "'");
              }
            }
          }
          fail("Value Objects must be immutable and should not have setter methods.", violations);
        });
  }

  public static DcaRule valueObjectsAreRecordsOrHaveAttributeEquality() {
    return DcaRule.check(
        "DCA-TAC-012",
        "Value Objects must be records or immutable classes with attribute equality",
        "A record grants attribute-based equality for free; a hand-written Value Object class must"
            + " override equals and hashCode itself to compare by its attributes",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass valueObject : concreteClassesAssignableTo(arch, Value.class)) {
            if (valueObject.isRecord() || valueObject.isEnum()) {
              continue;
            }
            if (!TypeInspection.declaresAttributeEquality(valueObject)) {
              violations.add(
                  valueObject.getName()
                      + " is a non-record Value Object without its own equals(Object)/hashCode()");
            }
          }
          fail(
              "Value Objects are records by preference; an immutable class is allowed, but it must"
                  + " implement attribute equality itself.",
              violations);
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Repository pattern
  // ---------------------------------------------------------------------------------------------

  public static DcaRule repositoryInterfacesExtendMarker(DcaLayout layout) {
    return DcaRule.of(
        "DCA-TAC-013",
        "Repository Interfaces should extend Repository Marker Interface",
        "Repository interfaces should extend Repository marker interface",
        arch ->
            classes()
                .that()
                .resideInAnyPackage(arch.allApplicationPatterns())
                .and()
                .areInterfaces()
                .and()
                .haveSimpleNameEndingWith(REPOSITORY_SUFFIX)
                .and()
                .doNotHaveSimpleName(REPOSITORY_SUFFIX)
                .should()
                .beAssignableTo(Repository.class)
                .allowEmptyShould(true));
  }

  public static DcaRule repositoryInterfacesResideInSharedOutputPorts(DcaLayout layout) {
    return DcaRule.of(
        "DCA-TAC-014",
        "Repository interfaces must reside in the application layer's shared output-port package",
        "Repository interfaces are output ports in the application layer (Hexagonal Architecture)",
        arch ->
            // areAssignableTo, not implement: ArchUnit's implement() matches non-interfaces only.
            classes()
                .that()
                .areInterfaces()
                .and()
                .areAssignableTo(Repository.class)
                .and()
                .doNotHaveSimpleName(REPOSITORY_SUFFIX)
                .should()
                .resideInAnyPackage(arch.allSharedOutputPortPatterns())
                .allowEmptyShould(true));
  }

  public static DcaRule repositoryImplementationsResideInOutgoingAdapters(DcaLayout layout) {
    return DcaRule.of(
        "DCA-TAC-015",
        "Repository Implementations must reside in adapter.outgoing package",
        "Repository implementations are outgoing adapters in bounded contexts",
        arch ->
            classes()
                .that()
                .areNotInterfaces()
                .and()
                .areAssignableTo(Repository.class)
                .should()
                .resideInAnyPackage(arch.allOutgoingAdapterPatterns())
                .allowEmptyShould(true));
  }

  public static DcaRule repositoriesOnlyForAggregateRoots() {
    return DcaRule.check(
        "DCA-TAC-016",
        "Repositories must only exist for Aggregate Roots",
        "A repository is the collection of one aggregate type; a repository for an entity would"
            + " let callers bypass the root that guards the aggregate's invariants",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass repository : repositoryInterfaces(arch)) {
            String repoName = repository.getSimpleName();
            if (!repoName.endsWith(REPOSITORY_SUFFIX)) {
              continue;
            }
            String aggregateName =
                repoName.substring(0, repoName.length() - REPOSITORY_SUFFIX.length());
            String context = arch.rootContextPackage(repository.getPackageName());
            List<JavaClass> candidates =
                arch.classes().stream()
                    .filter(c -> c.getSimpleName().equals(aggregateName))
                    .filter(
                        c ->
                            context == null
                                || context.equals(arch.rootContextPackage(c.getPackageName())))
                    .collect(Collectors.toList());
            if (candidates.isEmpty()) {
              violations.add(
                  repository.getName()
                      + " refers to '"
                      + aggregateName
                      + "' which cannot be resolved in its bounded context ("
                      + (context == null ? "outside base package" : context)
                      + ") - name the repository after the aggregate root it manages");
              continue;
            }
            for (JavaClass candidate : candidates) {
              if (!candidate.isAssignableTo(AggregateRoot.class)) {
                violations.add(
                    repository.getName()
                        + " exists for "
                        + candidate.getName()
                        + " which does not implement AggregateRoot");
              }
            }
          }
          fail(
              "Repositories should only exist for Aggregate Roots, not for Entities (DDD pattern).",
              violations);
        });
  }

  public static DcaRule repositoriesReturnNoNonRootEntities() {
    return DcaRule.check(
        "DCA-TAC-017",
        "Repository methods must not return non-root Entities",
        "A caller receiving an Entity that is not an Aggregate Root could mutate part of an"
            + " aggregate without passing its root, so the root's invariants would never run",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass repository : repositoryInterfaces(arch)) {
            for (JavaMethod method : repository.getMethods()) {
              for (JavaClass type : TypeInspection.involvedTypes(method.getReturnType())) {
                if (type.isAssignableTo(Entity.class)
                    && !type.isAssignableTo(AggregateRoot.class)) {
                  violations.add(
                      repository.getName()
                          + "."
                          + method.getName()
                          + " exposes "
                          + type.getName()
                          + ", an Entity that is not an Aggregate Root");
                }
              }
            }
          }
          fail(
              "Repository methods must not expose an Entity that is not an Aggregate Root: a caller"
                  + " could mutate part of an aggregate without passing its root (DDD pattern).",
              violations);
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Store pattern (Repository's sibling for non-aggregate operational data)
  // ---------------------------------------------------------------------------------------------

  public static DcaRule storeInterfacesExtendStoreMarker() {
    return DcaRule.of(
        "DCA-TAC-018",
        "Store interfaces must extend the Store marker, not Repository",
        "Stores extend the Store marker; Repository is reserved for Aggregate Roots",
        arch ->
            classes()
                .that()
                .areInterfaces()
                .and()
                .haveSimpleNameEndingWith(STORE_SUFFIX)
                .and()
                .doNotHaveSimpleName(STORE_SUFFIX)
                .should()
                .beAssignableTo(Store.class)
                .andShould()
                .notBeAssignableTo(Repository.class)
                .allowEmptyShould(true));
  }

  public static DcaRule storeInterfacesResideInSharedOutputPorts(DcaLayout layout) {
    return DcaRule.of(
        "DCA-TAC-019",
        "Store interfaces must reside in the application layer's shared output-port package",
        "Store interfaces are output ports in the application layer (Hexagonal Architecture)",
        arch ->
            classes()
                .that()
                .areInterfaces()
                .and()
                .areAssignableTo(Store.class)
                .and()
                .doNotHaveSimpleName(STORE_SUFFIX)
                .should()
                .resideInAnyPackage(arch.allSharedOutputPortPatterns())
                .allowEmptyShould(true));
  }

  public static DcaRule storeImplementationsResideInOutgoingAdapters(DcaLayout layout) {
    return DcaRule.of(
        "DCA-TAC-020",
        "Store implementations must reside in the adapter.outgoing package",
        "Store implementations are outgoing adapters in bounded contexts",
        arch ->
            classes()
                .that()
                .areNotInterfaces()
                .and()
                .areAssignableTo(Store.class)
                .should()
                .resideInAnyPackage(arch.allOutgoingAdapterPatterns())
                .allowEmptyShould(true));
  }

  public static DcaRule storeInterfacesHaveNoRepositorySemantics() {
    return DcaRule.check(
        "DCA-TAC-021",
        "Store interfaces must not declare findById or save methods",
        "findById/save are Repository semantics; a Store that has them is a Repository wearing the"
            + " wrong name, and the stored object should then be an Aggregate Root",
        arch -> {
          List<String> violations = new ArrayList<>();
          for (JavaClass store : storeInterfaces(arch)) {
            for (JavaMethod method : store.getMethods()) {
              if (REPOSITORY_METHOD_NAMES.contains(method.getName())) {
                violations.add(
                    store.getFullName()
                        + "."
                        + method.getName()
                        + "() - Repository semantics on a Store");
              }
            }
          }
          fail(
              "Store interfaces use record/count/exists semantics, not findById/save."
                  + " Fix: rename to *Repository if the stored object is an Aggregate Root,"
                  + " otherwise rename the methods to record(...), count(...), exists(...).",
              violations);
        });
  }

  // ---------------------------------------------------------------------------------------------
  // Enriched domain model pattern
  // ---------------------------------------------------------------------------------------------

  public static DcaRule enrichedModelsAreValueRecords(DcaLayout layout) {
    return DcaRule.of(
        "DCA-TAC-022",
        "Enriched Domain Models must be Value Object records",
        "Enriched domain models are immutable read projections and must be records implementing"
            + " Value",
        arch ->
            classes()
                .that()
                .haveSimpleNameStartingWith("Enriched")
                .and()
                .resideInAnyPackage(arch.allDomainModelPatterns())
                .and()
                .doNotImplement(Factory.class)
                .should()
                .beRecords()
                .andShould()
                .beAssignableTo(Value.class)
                .allowEmptyShould(true));
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  private static List<JavaClass> concreteClassesAssignableTo(
      DcaArchitecture arch, Class<?> marker) {
    return classesMatching(arch, c -> c.isAssignableTo(marker) && !c.isInterface());
  }

  private static List<JavaClass> nonRootEntities(DcaArchitecture arch) {
    return classesMatching(
        arch,
        c ->
            c.isAssignableTo(Entity.class)
                && !c.isAssignableTo(AggregateRoot.class)
                && !c.isInterface());
  }

  private static List<JavaClass> repositoryInterfaces(DcaArchitecture arch) {
    return classesMatching(
        arch,
        c ->
            c.isAssignableTo(Repository.class)
                && c.isInterface()
                && !c.getSimpleName().equals(REPOSITORY_SUFFIX));
  }

  private static List<JavaClass> storeInterfaces(DcaArchitecture arch) {
    return classesMatching(
        arch,
        c ->
            c.isAssignableTo(Store.class)
                && c.isInterface()
                && !c.getSimpleName().equals(STORE_SUFFIX));
  }

  private static List<JavaClass> classesMatching(
      DcaArchitecture arch, Predicate<JavaClass> filter) {
    return arch.classes().stream().filter(filter).collect(Collectors.toList());
  }

  private static boolean isConcreteAggregateRoot(JavaClass type) {
    return type.isAssignableTo(AggregateRoot.class) && !type.isInterface();
  }

  private static boolean isConcreteNonRootEntity(JavaClass type) {
    return type.isAssignableTo(Entity.class)
        && !type.isAssignableTo(AggregateRoot.class)
        && !type.isInterface();
  }

  private static boolean isSetter(JavaMethod method) {
    String name = method.getName();
    return name.startsWith("set")
        && name.length() > 3
        && Character.isUpperCase(name.charAt(3))
        && method.getRawParameterTypes().size() == 1
        && method.getRawReturnType().getName().equals("void");
  }

  /**
   * A field whose own type is the aggregate being inspected — a parent, a root, a predecessor — is
   * a self-reference and tolerated. A container of that type is not: its elements are
   * <em>other</em> instances of the aggregate, referenced by identity like any other aggregate.
   */
  private static boolean isSelfReference(JavaClass aggregate, JavaField field, JavaClass involved) {
    return involved.equals(aggregate) && involved.equals(field.getRawType());
  }

  /**
   * {@code Owner has field 'f' of type X} when the field's own type is the offender, {@code Owner
   * has field 'f' containing X} when it is hidden in a container or type argument.
   */
  private static String fieldDescription(JavaClass owner, JavaField field, JavaClass involved) {
    String relation = involved.equals(field.getRawType()) ? "' of type " : "' containing ";
    return owner.getName() + " has field '" + field.getName() + relation + involved.getName();
  }

  private static void fail(String message, List<String> violations) {
    if (!violations.isEmpty()) {
      throw new DcaRuleViolation(message, violations);
    }
  }
}
