package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaTypeVariable;
import com.tngtech.archunit.lang.conditions.ArchConditions;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaMarkers;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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

  /**
   * Write vocabulary a Store must not declare, compared case-insensitively so that the same six
   * names are matched in both languages - {@code save} and {@code SaveAsync} are the same name.
   */
  private static final Set<String> REPOSITORY_METHOD_NAMES =
      Set.of("save", "deletebyid", "delete", "saveasync", "deletebyidasync", "deleteasync");

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
            aggregateRootsHaveNoPublicConstructors(),
            valueObjectsHaveOneFactoryPerOrigin(),
            domainHoldsNoBarePrimitives());
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
                    .that(arch.inDomainModel())
                    .and()
                    .haveSimpleNameEndingWith(layout.aggregateRootSuffix())
                    .and()
                    .areNotInterfaces()
                    .and()
                    .doNotHaveSimpleName(layout.aggregateRootSuffix())
                    .should(ArchConditions.be(arch.layout().markers().is("aggregateRoot")))
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes in <module>.domain.model.. of every module root whose "
                + "simple name ends with AggregateRoot, the marker of the aggregate-root role "
                + "itself excluded.")
        .checking(
            "The class is assignable to the aggregate-root role - through the marker or an "
                + "intermediate base class. Only the name suffix triggers "
                + "selection - an aggregate root not named *AggregateRoot is never reported, and "
                + "an empty selection passes.");
  }

  public static DcaRule aggregateRootsHoldNoOutputPorts() {
    return DcaRule.check(
            "DCA-TAC-002",
            "Aggregate Roots must not hold references to Repositories or other Output Ports",
            "Aggregates are persistence-ignorant: use cases retrieve facts; external calculations belong"
                + " in domain services over supplied snapshots. Review callback parameters manually; this field check cannot prove semantic responsibility",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass aggregate :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("aggregateRoot"))) {
                for (JavaField field : aggregate.getAllFields()) {
                  JavaClass fieldType = field.getRawType();
                  if (arch.layout().markers().is("repository").test(fieldType)
                      || arch.layout().markers().is("outputPort").test(fieldType)) {
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
                  "Aggregates must not have injected repositories or output ports; pass facts instead.",
                  violations);
            })
        .selecting(
            "Non-interface classes anywhere under scan assignable to AggregateRoot, "
                + "abstract ones included.")
        .checking(
            "No field of the class - inherited and static ones included, a static port breaks "
                + "persistence ignorance just the same - has a raw type assignable to Repository or to "
                + "any other OutputPort. Only the raw type is "
                + "inspected; a port hidden in a generic type argument is not seen. A port passed "
                + "as a method parameter is not a field and passes.");
  }

  public static DcaRule aggregateRootsReferenceOtherAggregatesById() {
    return DcaRule.check(
            "DCA-TAC-003",
            "Aggregate Roots must not have fields with other Aggregate Root types",
            "Vernon's Aggregate Design Rule #3: reference other Aggregates by identity to keep"
                + " aggregate boundaries and transactional consistency intact",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass aggregate :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("aggregateRoot"))) {
                for (JavaField field : TypeInspection.instanceFields(aggregate)) {
                  for (JavaClass involved : TypeInspection.involvedTypes(field, aggregate)) {
                    if (isConcreteAggregateRoot(involved, arch.layout().markers())) {
                      violations.add(
                          fieldDescription(aggregate, field, involved)
                              + " which is another aggregate root");
                    }
                  }
                }
              }
              fail(
                  "Aggregates must reference other aggregates by ID only (Vernon's Rule #3).",
                  violations);
            })
        .selecting(
            "Non-interface classes anywhere under scan assignable to AggregateRoot, "
                + "abstract ones included.")
        .checking(
            "No instance field - inherited ones included, static ones excluded - involves AggregateRoot through its type, arrays or nested generic arguments. Same-type references and interfaces extending the marker are included. Interfaces that do not extend the marker are invisible; references by id are valid.");
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
              for (JavaClass entity :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("entity"))) {
                if (entity.getModifiers().contains(JavaModifier.ABSTRACT)) {
                  continue;
                }
                boolean hasIdField =
                    entity.getAllFields().stream()
                        .anyMatch(f -> arch.layout().markers().is("id").test(f.getRawType()));
                if (!hasIdField) {
                  violations.add(
                      entity.getName()
                          + " has no field whose type implements "
                          + simpleName(arch.layout().markers().id()));
                }
              }
              fail(
                  "Entities must have an identity field typed as an Id value object (DDD pattern).",
                  violations);
            })
        .selecting(
            "Non-interface, non-abstract classes anywhere under scan assignable to Entity - "
                + "aggregate roots included, since AggregateRoot extends Entity.")
        .checking(
            "At least one field - inherited ones included - has a raw type assignable to "
                + "the Id marker. A String or UUID identity does not count, and the field's name "
                + "plays no role.");
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
                String root = arch.moduleRootOf(entity.getPackageName());
                for (var constructor : entity.getConstructors()) {
                  for (var call : constructor.getCallsOfSelf()) {
                    JavaClass caller = call.getOriginOwner();
                    String domain =
                        root == null ? "" : root + "." + arch.layout().domainSubpackage();
                    boolean sameDomain =
                        root != null
                            && root.equals(arch.moduleRootOf(caller.getPackageName()))
                            && (caller.getPackageName().equals(domain)
                                || caller.getPackageName().startsWith(domain + "."));
                    boolean role =
                        arch.layout().markers().is("aggregateRoot").test(caller)
                            || arch.layout().markers().is("entity").test(caller)
                            || arch.layout().markers().is("factory").test(caller);
                    if (!caller.equals(entity) && !(sameDomain && role)) {
                      violations.add(
                          caller.getName()
                              + " constructs entity "
                              + entity.getName()
                              + " outside its domain construction boundary");
                    }
                  }
                }
              }
              fail(
                  "Entities are constructed by their own domain aggregate, entity or factory.",
                  violations);
            })
        .selecting("Constructor calls to non-root Entity types, records included.")
        .checking(
            "The caller is the entity itself or an AggregateRoot, Entity or Factory in the same context domain layer. Same aggregate ownership is not decidable: another aggregate in that context passes and needs review. Reconstitution goes through an aggregate or factory; reflection is not inspected.");
  }

  public static DcaRule domainModelHasNoPublicSetters() {
    return DcaRule.check(
            "DCA-TAC-006",
            "Domain model classes must not have public setter methods",
            "Behavior-rich domain models change state through intention-revealing methods from the"
                + " ubiquitous language, never through public setters",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass domainClass :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("entity"))) {
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
            })
        .selecting(
            "Non-interface classes anywhere under scan assignable to Entity - aggregate "
                + "roots included.")
        .checking(
            "No method - inherited ones included - is a public setter: a name starting with "
                + "set followed by an upper-case letter, exactly one parameter and return type "
                + "void. A non-public setter, a fluent setter returning the instance, or a "
                + "set-prefixed method with zero or two parameters does not count.");
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
                    if (isConcreteAggregateRoot(involved, arch.layout().markers())) {
                      violations.add(
                          fieldDescription(entity, field, involved)
                              + " which is an aggregate root");
                    }
                  }
                }
              }
              fail(
                  "Entities must not contain references to aggregate roots (reference by ID only).",
                  violations);
            })
        .selecting(
            "Non-interface classes anywhere under scan assignable to Entity but not to "
                + "AggregateRoot; records and abstract classes included.")
        .checking(
            "No instance state, including inherited state, arrays and nested generic arguments, involves AggregateRoot. Same-type references and interfaces extending the marker are included. Interfaces that do not extend the marker are invisible; references by id are valid.");
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
              for (JavaClass valueObject :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("value"))) {
                for (JavaField field : TypeInspection.instanceFields(valueObject)) {
                  for (JavaClass involved : TypeInspection.involvedTypes(field, valueObject)) {
                    if (isConcreteAggregateRoot(involved, arch.layout().markers())) {
                      violations.add(
                          fieldDescription(valueObject, field, involved)
                              + " which is an aggregate root");
                    }
                    if (isConcreteNonRootEntity(involved, arch.layout().markers())) {
                      violations.add(
                          fieldDescription(valueObject, field, involved) + " which is an entity");
                    }
                  }
                }
              }
              fail(
                  "Value Objects must only contain other Value Objects or primitives (Vernon's DDD).",
                  violations);
            })
        .selecting(
            "Non-interface classes anywhere under scan assignable to Value - records, enums "
                + "and hand-written classes alike.")
        .checking(
            "No instance state, including inherited state, arrays and nested generic arguments, involves AggregateRoot or Entity. Same-type references and interfaces extending the marker are included. Interfaces that do not extend the marker are invisible; references by id are valid.");
  }

  public static DcaRule valueObjectClassesAreFinal(DcaLayout layout) {
    return DcaRule.of(
            "DCA-TAC-009",
            "Value Object classes should be final (immutability)",
            "Value objects should be immutable (final classes) - Vernon's DDD recommendation",
            arch ->
                classes()
                    .that(arch.inDomainModel())
                    .and(arch.layout().markers().is("value"))
                    .and()
                    .areNotInterfaces()
                    .and()
                    .areNotRecords()
                    .and()
                    .areNotEnums()
                    .should()
                    .haveModifier(JavaModifier.FINAL)
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface, non-record, non-enum classes in <module>.domain.model.. of every "
                + "module root that are assignable to Value.")
        .checking(
            "The class carries the final modifier. Records, interfaces and enums are not "
                + "selected, so a record value object always passes and an enum value object is "
                + "never reported - an enum with constant-specific class bodies is compiled "
                + "abstract and cannot be made final. A value object outside "
                + "<module>.domain.model.. is never reported, and an empty selection passes.");
  }

  public static DcaRule valueObjectFieldsAreFinal() {
    return DcaRule.check(
            "DCA-TAC-010",
            "Value Object fields must be final (shallow immutability)",
            "Records have implicitly final fields and enums are immutable by design; a hand-written"
                + " value class must make every instance field final itself",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass valueObject :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("value"))) {
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
                  "Value Object fields must be final for shallow immutability (Vernon's DDD).",
                  violations);
            })
        .selecting(
            "Non-interface, non-record, non-enum classes anywhere under scan assignable to "
                + "Value.")
        .checking(
            "Every field - inherited ones included - is final or static. A non-final "
                + "instance field is reported; static fields are not part of the object's state "
                + "and pass.");
  }

  public static DcaRule valueObjectsHaveNoSetters() {
    return DcaRule.check(
            "DCA-TAC-011",
            "Value Objects must not have setter methods",
            "Value Objects are immutable; state changes produce a new instance instead of mutating",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass valueObject :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("value"))) {
                for (JavaMethod method : valueObject.getAllMethods()) {
                  if (isSetter(method)) {
                    violations.add(
                        valueObject.getName() + " has setter method '" + method.getName() + "'");
                  }
                }
              }
              fail(
                  "Value Objects must be immutable and should not have setter methods.",
                  violations);
            })
        .selecting(
            "Non-interface classes anywhere under scan assignable to Value - records and "
                + "enums included.")
        .checking(
            "No method - inherited ones included - is a setter: a name starting with set "
                + "followed by an upper-case letter, exactly one parameter and return type void, "
                + "regardless of visibility. A wither that returns a new instance is not a "
                + "setter.");
  }

  public static DcaRule valueObjectsAreRecordsOrHaveAttributeEquality() {
    return DcaRule.check(
            "DCA-TAC-012",
            "Value Objects must be records or immutable classes with attribute equality",
            "A record grants attribute-based equality for free; a hand-written Value Object class must"
                + " override equals and hashCode itself to compare by its attributes",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass valueObject :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("value"))) {
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
            })
        .selecting(
            "Non-interface, non-record, non-enum classes anywhere under scan assignable to "
                + "Value.")
        .checking(
            "The class, or a superclass other than Object, overrides both boolean "
                + "equals(Object) and int hashCode() with exactly those signatures. An overload "
                + "such as equals(Money) does not count, and overriding only one of the two is "
                + "reported.");
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
                    .that(arch.inApplication())
                    .and()
                    .areInterfaces()
                    .and()
                    .haveSimpleNameEndingWith(arch.layout().repositorySuffix())
                    .and()
                    .doNotHaveSimpleName(arch.layout().repositorySuffix())
                    .should(ArchConditions.be(arch.layout().markers().is("repository")))
                    .allowEmptyShould(true))
        .selecting(
            "Interfaces in <module>.application.. of every module root whose simple name "
                + "ends with Repository, the marker Repository itself excluded.")
        .checking(
            "The interface is assignable to the Repository marker. A *Repository interface "
                + "outside <module>.application.. is not selected; an empty selection passes.");
  }

  public static DcaRule repositoryInterfacesResideInSharedOutputPorts(DcaLayout layout) {
    return DcaRule.of(
            "DCA-TAC-014",
            "Repository interfaces must reside in the application layer (local to a use case or shared)",
            "Repository interfaces are output ports in the application layer (Hexagonal Architecture)",
            arch ->
                // areAssignableTo, not implement: ArchUnit's implement() matches non-interfaces
                // only.
                classes()
                    .that()
                    .areInterfaces()
                    .and(arch.layout().markers().is("repository"))
                    .and()
                    .doNotHaveSimpleName(arch.layout().repositorySuffix())
                    .should(ArchConditions.be(arch.inApplication()))
                    .allowEmptyShould(true))
        .selecting(
            "Interfaces anywhere under scan assignable to Repository, whatever their name, "
                + "the marker Repository itself excluded.")
        .checking(
            "The interface resides in <module>.application.. of some module root, "
                + "the shared kernel's included. Implementations are not selected; an empty "
                + "selection passes.");
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
                    .and(arch.layout().markers().is("repository"))
                    .should(ArchConditions.be(arch.inOutgoingAdapter()))
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere under scan assignable to Repository, abstract "
                + "base classes included.")
        .checking(
            "The class resides in <module>.adapter.outgoing.. of some module root. An "
                + "implementation in any other package under scan - a test double, say - is "
                + "reported; an empty selection passes. An implementation that carries the "
                + "repository role and is also named *Repository is reported by DCA-HEX-008 as "
                + "well: that rule finds unmarked implementations by name, and the two "
                + "populations overlap where a project does both.");
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
                if (!repoName.endsWith(arch.layout().repositorySuffix())) {
                  continue;
                }
                Optional<JavaType> bound =
                    boundAggregateArgument(repository, arch.layout().markers().repository());
                if (bound.isPresent()) {
                  if (bound.get() instanceof JavaTypeVariable) {
                    // A generic intermediate port binds no aggregate of its own.
                    continue;
                  }
                  JavaClass aggregate = bound.get().toErasure();
                  if (!arch.layout().markers().is("aggregateRoot").test(aggregate)) {
                    violations.add(
                        repository.getName()
                            + " binds "
                            + aggregate.getName()
                            + " which does not implement AggregateRoot");
                  } else if (!repoName.equals(
                      aggregate.getSimpleName() + arch.layout().repositorySuffix())) {
                    violations.add(
                        repository.getName()
                            + " binds "
                            + aggregate.getName()
                            + " but is named "
                            + repoName
                            + " - name it "
                            + aggregate.getSimpleName()
                            + arch.layout().repositorySuffix());
                  }
                  continue;
                }
                String aggregateName =
                    repoName.substring(
                        0, repoName.length() - arch.layout().repositorySuffix().length());
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
                  if (!arch.layout().markers().is("aggregateRoot").test(candidate)) {
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
            })
        .selecting(
            "Interfaces anywhere under scan assignable to Repository whose simple name ends "
                + "with Repository, the marker Repository itself excluded.")
        .checking(
            "The aggregate is read from the type argument the interface binds: the first argument"
                + " of the parameterised Repository marker it extends, or of an intermediate port"
                + " that is itself assignable to the marker. That type must be assignable to"
                + " AggregateRoot, and the interface's simple name must be that type's simple name"
                + " plus 'Repository' - so a repository bound to one aggregate and named after"
                + " another is reported. An unresolved type argument is skipped: a generic"
                + " intermediate port such as AuditedRepository<T, ID> binds no aggregate of its"
                + " own. When the marker is not generic or is used raw, the aggregate is resolved"
                + " by name instead: among all classes under scan with the interface's simple name"
                + " minus 'Repository' and in the same context - the nearest enclosing package"
                + " annotated with @BoundedContext or @SharedKernel, falling back to the first"
                + " segment below the base package; anywhere when the interface lies outside the"
                + " base package - at least one must exist and every one must be assignable to"
                + " AggregateRoot. The interface's methods play no role.",
            "name the repository after the aggregate root it binds");
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
                    if (arch.layout().markers().is("entity").test(type)
                        && !arch.layout().markers().is("aggregateRoot").test(type)) {
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
            })
        .selecting(
            "Interfaces anywhere under scan assignable to Repository, the marker Repository "
                + "itself excluded.")
        .checking(
            "No return type of a method declared on the interface itself - walked through "
                + "raw type, array component, generic type arguments and wildcard bounds, so "
                + "Optional<T>, List<T> and Map<K,V> are seen through - involves a class "
                + "assignable to Entity that is not also assignable to AggregateRoot. Inherited "
                + "methods and parameter types are not inspected.");
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
                    .haveSimpleNameEndingWith(arch.layout().storeSuffix())
                    .and()
                    .doNotHaveSimpleName(arch.layout().storeSuffix())
                    .and()
                    .doNotHaveFullyQualifiedName(arch.layout().markers().store())
                    .and(DescribedPredicate.not(arch.inAdapter()))
                    .and(DescribedPredicate.not(arch.inInfrastructure()))
                    .should(ArchConditions.be(arch.layout().markers().is("store")))
                    .andShould(
                        ArchConditions.be(
                            DescribedPredicate.not(arch.layout().markers().is("repository"))))
                    .allowEmptyShould(true))
        .selecting(
            "Interfaces under scan whose simple name ends with Store, the marker Store itself"
                + " and the type the store role names excluded, outside every adapter and"
                + " infrastructure package - an interface there is an adapter's own abstraction,"
                + " never a port.")
        .checking(
            "The interface is assignable to the Store marker and not assignable to "
                + "Repository; both must hold. An interface not named *Store is never reported; "
                + "an empty selection passes.");
  }

  public static DcaRule storeInterfacesResideInSharedOutputPorts(DcaLayout layout) {
    return DcaRule.of(
            "DCA-TAC-019",
            "Store interfaces must reside in the application layer (local to a use case or shared)",
            "Store interfaces are output ports in the application layer (Hexagonal Architecture)",
            arch ->
                classes()
                    .that()
                    .areInterfaces()
                    .and(arch.layout().markers().is("store"))
                    .and()
                    .doNotHaveSimpleName(arch.layout().storeSuffix())
                    .should(ArchConditions.be(arch.inApplication()))
                    .allowEmptyShould(true))
        .selecting(
            "Interfaces anywhere under scan assignable to Store, whatever their name, the "
                + "marker Store itself excluded.")
        .checking(
            "The interface resides in <module>.application.. of some module root, "
                + "the shared kernel's included. Implementations are not selected; an empty "
                + "selection passes.");
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
                    .and(arch.layout().markers().is("store"))
                    .should(ArchConditions.be(arch.inOutgoingAdapter()))
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere under scan assignable to Store, abstract base "
                + "classes included.")
        .checking(
            "The class resides in <module>.adapter.outgoing.. of some module root; an empty "
                + "selection passes.");
  }

  public static DcaRule storeInterfacesHaveNoRepositorySemantics() {
    return DcaRule.check(
            "DCA-TAC-021",
            "Store interfaces must not declare save or delete methods",
            "save/delete are Repository semantics; a Store that has them is a Repository wearing the"
                + " wrong name, and the stored object should then be an Aggregate Root",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass store : storeInterfaces(arch)) {
                for (JavaMethod method : store.getMethods()) {
                  if (REPOSITORY_METHOD_NAMES.contains(
                      method.getName().toLowerCase(java.util.Locale.ROOT))) {
                    violations.add(
                        store.getFullName()
                            + "."
                            + method.getName()
                            + "() - Repository semantics on a Store");
                  }
                }
              }
              fail(
                  "Store interfaces use record/count/exists semantics, not save/delete."
                      + " Fix: rename to *Repository if the stored object is an Aggregate Root,"
                      + " otherwise rename the methods to record(...), count(...), exists(...).",
                  violations);
            })
        .selecting(
            "Interfaces anywhere under scan assignable to Store, the marker Store itself "
                + "excluded.")
        .checking(
            "No method declared on the interface itself is named save, deleteById, delete "
                + "or one of their asynchronous forms saveAsync, deleteByIdAsync, "
                + "deleteAsync - matched by name alone, parameters and return type "
                + "disregarded and case ignored, so both languages match the same six names. Inherited methods "
                + "are not inspected, and no particular vocabulary (record, count, exists) "
                + "is required. The six names are fixed and are not part of the marker "
                + "roles: a vocabulary that writes under another name is selected and then "
                + "found to declare no write.");
  }

  /**
   * The aggregate's sibling of DCA-TAC-005: an entity is guarded by its root, the root guards
   * itself by naming the ways it comes into being.
   */
  public static DcaRule aggregateRootsHaveNoPublicConstructors() {
    return DcaRule.check(
            "DCA-TAC-023",
            "Aggregate Roots must not declare public constructors",
            "An aggregate comes into being in two ways - created as a business fact or reconstituted"
                + " from storage - and each is a named factory method. A public constructor is a"
                + " third, unnamed way that skips the creation invariants and the event creation"
                + " raises",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass root : aggregateRootClasses(arch)) {
                for (var constructor : root.getConstructors()) {
                  if (constructor.getModifiers().contains(JavaModifier.PUBLIC)) {
                    violations.add(
                        root.getName()
                            + " declares public constructor "
                            + constructor.getFullName());
                  }
                }
              }
              fail(
                  "Aggregate Roots are created and reconstituted through named factory methods."
                      + " Fix: make the constructor private and add a static create(...) for a new"
                      + " aggregate and a static reconstitute(...) for the outgoing adapter that"
                      + " loads one.",
                  violations);
            })
        .selecting(
            "Concrete, non-record classes anywhere under scan assignable to AggregateRoot. Abstract"
                + " base classes are not selected - their constructors are reachable only from a"
                + " subclass. Records are not selected: the language fixes a record's canonical"
                + " constructor at the record's own visibility, so a record root guards its"
                + " invariants in that constructor instead.")
        .checking(
            "No constructor declared by the class itself is public. Private, package-private and"
                + " protected constructors pass - a persistence framework that needs a no-argument"
                + " constructor gets a non-public one. Who calls the factory methods is not"
                + " checked. An empty selection passes.");
  }

  /**
   * DCA-TAC-024. Two factories with one parameter signature take the same input and differ only in
   * the checks they run - the caller has to guess which. The neutral {@code of} is the one that
   * names no origin; two named factories with equal parameters ({@code fromInput(String)}, {@code
   * fromStored(String)}) name theirs and pass.
   */
  public static DcaRule valueObjectsHaveOneFactoryPerOrigin() {
    return DcaRule.check(
            "DCA-TAC-024",
            "Value Objects must not declare a neutral factory beside an origin factory with the same parameters",
            "A value built from input and a value rebuilt from storage are two origins with"
                + " different checks - validated and derived on the way in, taken as given on the"
                + " way back - and each has its own named factory. A second factory that takes the"
                + " same parameters, such as a neutral of(...) beside them, leaves the caller to"
                + " guess which checks run",
            arch -> {
              List<String> violations = new ArrayList<>();
              for (JavaClass value :
                  concreteClassesAssignableTo(arch, arch.layout().markers().is("value"))) {
                java.util.Map<List<String>, List<String>> bySignature =
                    new java.util.LinkedHashMap<>();
                for (JavaMethod method : value.getMethods()) {
                  Set<JavaModifier> modifiers = method.getModifiers();
                  if (modifiers.contains(JavaModifier.PUBLIC)
                      && modifiers.contains(JavaModifier.STATIC)
                      && !modifiers.contains(JavaModifier.SYNTHETIC)
                      && method.getRawReturnType().equals(value)
                      && !isEnumValueOf(value, method)) {
                    List<String> signature =
                        method.getRawParameterTypes().stream().map(JavaClass::getName).toList();
                    bySignature
                        .computeIfAbsent(signature, key -> new ArrayList<>())
                        .add(method.getName());
                  }
                }
                bySignature.forEach(
                    (signature, factories) -> {
                      if (factories.size() > 1 && factories.contains("of")) {
                        violations.add(
                            value.getName()
                                + " has "
                                + factories.size()
                                + " factories taking ("
                                + String.join(", ", signature)
                                + "): "
                                + String.join(", ", factories.stream().sorted().toList()));
                      }
                    });
              }
              fail(
                  "Value Objects must not declare a neutral factory beside an origin factory with the same parameters. Fix: remove the neutral of(...) and"
                      + " keep one named factory for each origin - from input, from storage.",
                  violations);
            })
        .experimental()
        .selecting(
            "Non-interface classes anywhere under scan assignable to Value - records, enums and"
                + " hand-written classes alike - and their public static methods whose raw return"
                + " type is the class itself.")
        .checking(
            "No factory named of takes the same raw parameter types, in the same sequence, as"
                + " another of these factories. Two named factories with equal parameters"
                + " (fromInput(String), fromStored(String)) name their origins and pass; factories"
                + " with different parameters pass, as do constructors, which are not"
                + " factories by this rule, static methods returning another type, and the"
                + " valueOf(String) the language declares on an enum. Whether a"
                + " factory's name says its origin is not checked. An empty selection passes.",
            "remove the neutral of(...) and keep one named factory for each origin - from input,"
                + " from storage");
  }

  /** The types a domain wraps in a value object instead of holding them bare. */
  private static final Set<String> BARE_TYPES =
      Set.of(
          "java.lang.String",
          "boolean",
          "java.lang.Boolean",
          "byte",
          "java.lang.Byte",
          "short",
          "java.lang.Short",
          "int",
          "java.lang.Integer",
          "long",
          "java.lang.Long",
          "float",
          "java.lang.Float",
          "double",
          "java.lang.Double",
          "char",
          "java.lang.Character",
          "java.time.Instant",
          "java.time.LocalDate",
          "java.time.LocalDateTime",
          "java.time.LocalTime",
          "java.time.OffsetDateTime",
          "java.time.ZonedDateTime",
          "java.util.Date",
          "java.util.UUID");

  /** The methods every class has from the platform; their signatures are not the model's. */
  private static final Set<String> PLATFORM_METHODS =
      Set.of("equals", "hashCode", "toString", "compareTo");

  /**
   * DCA-TAC-025. A string, a number, a flag or a moment that means something in the domain carries
   * its rules in a value object of its own - a postal code, a quantity, a status. Held bare, its
   * rules are repeated wherever it is used, or nowhere.
   */
  public static DcaRule domainHoldsNoBarePrimitives() {
    return DcaRule.check(
            "DCA-TAC-025",
            "Domain types must not hold bare primitives, strings or time types",
            "A value with rules of its own - a postal code, a quantity, a status, a moment, an"
                + " identifier - is a value object; held as a bare String, int, boolean, Instant or UUID its"
                + " validation is"
                + " repeated at every use or missing. A value object with several components holds"
                + " value objects too; only a wrapper - a value of exactly one component - holds the"
                + " primitive it wraps",
            arch -> {
              DcaMarkers markers = arch.layout().markers();
              Set<String> buildingBlocks = buildingBlockTypes(markers);
              List<String> violations = new ArrayList<>();
              for (JavaClass type :
                  classesMatching(
                      arch,
                      c ->
                          arch.inDomain().test(c)
                              && !c.getModifiers().contains(JavaModifier.SYNTHETIC))) {
                if (isWrapperValue(type, markers)) {
                  continue;
                }
                List<String> bare = new ArrayList<>();
                Set<String> exemptAccessors = new java.util.HashSet<>();
                for (JavaMethod method : type.getMethods()) {
                  if (implementsBuildingBlock(type, method, buildingBlocks)) {
                    exemptAccessors.add(method.getName());
                  }
                }
                Set<String> reportedFields = new java.util.HashSet<>();
                for (JavaField field : type.getFields()) {
                  if (field.getModifiers().contains(JavaModifier.STATIC)
                      || field.getModifiers().contains(JavaModifier.SYNTHETIC)
                      || exemptAccessors.contains(field.getName())) {
                    continue;
                  }
                  String found = bareTypesIn(field.getType());
                  if (!found.isEmpty()) {
                    bare.add("field " + field.getName() + " is " + found);
                    reportedFields.add(field.getName());
                  }
                }
                for (JavaMethod method : type.getMethods()) {
                  Set<JavaModifier> modifiers = method.getModifiers();
                  if (modifiers.contains(JavaModifier.SYNTHETIC)
                      || modifiers.contains(JavaModifier.BRIDGE)
                      || PLATFORM_METHODS.contains(method.getName())
                      || isEnumImplicit(type, method)
                      || exemptAccessors.contains(method.getName())) {
                    continue;
                  }
                  boolean accessorOfReportedField =
                      method.getRawParameterTypes().isEmpty()
                          && reportedFields.contains(method.getName());
                  String returnType = method.getRawReturnType().getName();
                  boolean predicate =
                      returnType.equals("boolean") || returnType.equals("java.lang.Boolean");
                  String returned = bareTypesIn(method.getReturnType());
                  if (!returned.isEmpty() && !accessorOfReportedField && !predicate) {
                    bare.add(method.getName() + "() returns " + returned);
                  }
                  List<JavaType> parameters = method.getParameterTypes();
                  for (int i = 0; i < parameters.size(); i++) {
                    String taken = bareTypesIn(parameters.get(i));
                    if (!taken.isEmpty()) {
                      bare.add(
                          method.getName() + "(...) takes " + taken + " as parameter " + (i + 1));
                    }
                  }
                }
                if (!bare.isEmpty()) {
                  violations.add(
                      type.getName() + ": " + String.join("; ", bare.stream().sorted().toList()));
                }
              }
              fail(
                  "Domain types must not hold bare primitives, strings or time types. Fix: wrap each"
                      + " in a value object that carries its rules - a value of one component"
                      + " may hold the primitive it wraps.",
                  violations);
            })
        .experimental()
        .selecting(
            "Classes, records, enums and interfaces in <module>.domain.. of every module root, the"
                + " shared kernel's domain included - their instance fields and their methods,"
                + " private ones included; constructors are not selected, the fields they fill are."
                + " Not selected: a wrapper value - a type assignable to the value or the id role"
                + " with exactly one instance field - with everything it declares; synthetic and"
                + " bridge members; equals, hashCode, toString and compareTo; the values() and"
                + " valueOf(String) the language declares on an enum; a method that implements a"
                + " method of a building block, such as a domain event's occurredOn() or a"
                + " specification's isSatisfiedBy, and the field of the same name behind it. A boolean"
                + " return is a question to the object - a predicate - and passes; a boolean field or"
                + " parameter does not.")
        .checking(
            "No selected field, return type or parameter type involves String, a primitive or its"
                + " box, or a point in time (java.time.Instant, LocalDate, LocalDateTime, LocalTime,"
                + " OffsetDateTime, ZonedDateTime, java.util.Date), nor a bare java.util.UUID - an"
                + " identifier says whose it is through a typed id - generic type arguments"
                + " included, so List<String> counts. BigDecimal, BigInteger, Duration and the"
                + " project's own types pass. A value object with several components is checked"
                + " like any other type: each component is a value object itself. A record's"
                + " accessor is not reported again beside its field. An empty selection passes.",
            "wrap the primitive in a value object that carries its rules; a value of one"
                + " component may hold it");
  }

  /** A value or an identifier of exactly one instance field: it wraps that field's primitive. */
  private static boolean isWrapperValue(JavaClass type, DcaMarkers markers) {
    if (!markers.is("value").test(type) && !markers.is("id").test(type)) {
      return false;
    }
    return type.getFields().stream()
            .filter(
                f ->
                    !f.getModifiers().contains(JavaModifier.STATIC)
                        && !f.getModifiers().contains(JavaModifier.SYNTHETIC))
            .count()
        == 1;
  }

  /** The type names of every building block the layout names. */
  private static Set<String> buildingBlockTypes(DcaMarkers markers) {
    return Set.of(
        markers.aggregateRoot(),
        markers.entity(),
        markers.value(),
        markers.id(),
        markers.domainEvent(),
        markers.integrationEvent(),
        markers.domainService(),
        markers.factory(),
        markers.specification(),
        markers.domainException(),
        markers.outputPort(),
        markers.repository(),
        markers.store());
  }

  /** Whether a building block the type implements declares a method of this name and arity. */
  private static boolean implementsBuildingBlock(
      JavaClass type, JavaMethod method, Set<String> buildingBlocks) {
    List<JavaClass> supertypes = new ArrayList<>(type.getAllRawInterfaces());
    supertypes.addAll(type.getAllRawSuperclasses());
    return supertypes.stream()
        .filter(s -> buildingBlocks.contains(s.getName()))
        .flatMap(s -> s.getMethods().stream())
        .anyMatch(
            m ->
                m.getName().equals(method.getName())
                    && m.getRawParameterTypes().size() == method.getRawParameterTypes().size());
  }

  /** The {@code values()} and {@code valueOf(String)} the language declares on every enum. */
  private static boolean isEnumImplicit(JavaClass type, JavaMethod method) {
    return type.isEnum()
        && (isEnumValueOf(type, method)
            || (method.getName().equals("values") && method.getRawParameterTypes().isEmpty()));
  }

  /** The bare types a declared type involves, generic arguments included; "" when none. */
  private static String bareTypesIn(JavaType type) {
    return type.getAllInvolvedRawTypes().stream()
        .map(JavaClass::getName)
        .filter(BARE_TYPES::contains)
        .map(name -> name.substring(name.lastIndexOf('.') + 1))
        .distinct()
        .sorted()
        .collect(Collectors.joining(", "));
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers
  // ---------------------------------------------------------------------------------------------

  /**
   * The {@code valueOf(String)} the language declares on every enum - not a factory of the model.
   */
  private static boolean isEnumValueOf(JavaClass type, JavaMethod method) {
    return type.isEnum()
        && method.getName().equals("valueOf")
        && method.getRawParameterTypes().size() == 1
        && method.getRawParameterTypes().get(0).getName().equals("java.lang.String");
  }

  private static List<JavaClass> concreteClassesAssignableTo(
      DcaArchitecture arch, DescribedPredicate<JavaClass> role) {
    return classesMatching(arch, c -> role.test(c) && !c.isInterface());
  }

  private static List<JavaClass> aggregateRootClasses(DcaArchitecture arch) {
    return classesMatching(
        arch,
        c ->
            arch.layout().markers().is("aggregateRoot").test(c)
                && !c.isInterface()
                && !c.getModifiers().contains(JavaModifier.ABSTRACT)
                && !c.isRecord());
  }

  private static List<JavaClass> nonRootEntities(DcaArchitecture arch) {
    return classesMatching(
        arch,
        c ->
            arch.layout().markers().is("entity").test(c)
                && !arch.layout().markers().is("aggregateRoot").test(c)
                && !c.isInterface());
  }

  /**
   * The aggregate type a repository interface binds: the first type argument of the parameterised
   * repository marker it extends, or of an intermediate port that is itself assignable to the
   * marker. Empty when the marker is not generic, when it is used raw, or when the binding only
   * becomes concrete further up a chain that substitutes type parameters - the rule then falls back
   * to resolving the aggregate by name.
   */
  private static Optional<JavaType> boundAggregateArgument(JavaClass type, String markerName) {
    for (JavaType candidate : type.getInterfaces()) {
      JavaClass erasure = candidate.toErasure();
      boolean isMarker = erasure.getName().equals(markerName) || erasure.isAssignableTo(markerName);
      if (isMarker
          && candidate instanceof JavaParameterizedType parameterized
          && !parameterized.getActualTypeArguments().isEmpty()) {
        return Optional.of(parameterized.getActualTypeArguments().get(0));
      }
    }
    return Optional.empty();
  }

  private static List<JavaClass> repositoryInterfaces(DcaArchitecture arch) {
    return classesMatching(
        arch,
        c ->
            arch.layout().markers().is("repository").test(c)
                && c.isInterface()
                && !c.getSimpleName().equals(arch.layout().repositorySuffix()));
  }

  private static List<JavaClass> storeInterfaces(DcaArchitecture arch) {
    return classesMatching(
        arch,
        c ->
            arch.layout().markers().is("store").test(c)
                && c.isInterface()
                && !c.getSimpleName().equals(arch.layout().storeSuffix()));
  }

  /** The simple name of a configured marker, for a message a reader has to recognise. */
  private static String simpleName(String fqn) {
    return fqn.substring(fqn.lastIndexOf('.') + 1);
  }

  private static List<JavaClass> classesMatching(
      DcaArchitecture arch, Predicate<JavaClass> filter) {
    return arch.classes().stream().filter(filter).collect(Collectors.toList());
  }

  private static boolean isConcreteAggregateRoot(JavaClass type, DcaMarkers markers) {
    return markers.is("aggregateRoot").test(type);
  }

  private static boolean isConcreteNonRootEntity(JavaClass type, DcaMarkers markers) {
    return markers.is("entity").test(type) && !markers.is("aggregateRoot").test(type);
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
