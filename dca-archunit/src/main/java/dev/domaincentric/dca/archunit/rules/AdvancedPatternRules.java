package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaMarkers;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEventType;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Advanced tactical DDD rules: domain events, integration events, domain services, factories and
 * specifications.
 *
 * <p>Reference: Eric Evans, <i>Domain-Driven Design</i> (Domain Events, Services, Factories,
 * Specifications); Vaughn Vernon, <i>Implementing DDD</i> (Domain Events for eventual consistency).
 */
public final class AdvancedPatternRules implements DcaRuleSet {

  private static final java.util.Set<String> SCHEMA_FIELDS =
      java.util.Set.of("schemaVersion", "eventVersion", "contractVersion");

  private final DcaLayout layout;
  private final List<DcaRule> rules;

  public AdvancedPatternRules(DcaLayout layout) {
    this.layout = Objects.requireNonNull(layout, "layout");
    this.rules =
        List.of(
            domainEventsAreRecords(),
            domainEventsResideInDomain(),
            domainEventsHaveNoFrameworkAnnotations(),
            integrationEventsAreAnnotatedWithIntegrationEventType(),
            integrationEventsHaveNoVersionField(),
            domainOnlyEventsHaveNoVersionField(),
            domainEventsHaveTimestampField(),
            domainServicesResideInDomain(),
            domainServicesHaveNoFrameworkAnnotations(),
            domainServicesAreStateless(),
            factoriesAreNamedFactory(),
            factoriesResideInDomain(),
            factoriesHaveNoFrameworkAnnotations(),
            factoriesAreStateless(),
            specificationsResideInDomain(),
            specificationsHaveNoFrameworkAnnotations());
  }

  @Override
  public String name() {
    return "advanced";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  // ============================================================================
  // DOMAIN EVENTS PATTERN
  // ============================================================================

  public DcaRule domainEventsAreRecords() {
    return DcaRule.of(
            "DCA-ADV-001",
            "Domain Events must implement DomainEvent and have immutable shape",
            "Domain events should have immutable state implementing DomainEvent (named in past tense,"
                + " e.g., ProductCreated, CartCleared)",
            arch ->
                classes()
                    .that()
                    .areAssignableTo(arch.layout().markers().domainEvent())
                    .and()
                    .areNotInterfaces()
                    .should(TypeInspection.haveImmutableShape())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to DomainEvent"
                + " - directly or through a supertype.")
        .checking(
            "The class is final or a record with final inherited instance fields and no instance setter methods - a name heuristic: set followed by an upper-case letter, with parameters, returning void (settle(x) is not a setter)."
                + " Referenced objects and collection contents are not inspected. Interfaces are excluded; an enum implementing DomainEvent is final by construction and passes.");
  }

  public DcaRule domainEventsResideInDomain() {
    return DcaRule.of(
            "DCA-ADV-002",
            "Domain Events must reside in domain package",
            "Domain events are part of the domain layer (named in past tense)",
            arch ->
                classes()
                    .that()
                    .areAssignableTo(arch.layout().markers().domainEvent())
                    .and()
                    .areNotInterfaces()
                    .should()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to DomainEvent.")
        .checking(
            "Each resides in a domain package of some module root (<module>.domain..). An event in an"
                + " application, adapter or infrastructure package is reported. An empty selection passes.");
  }

  public DcaRule domainEventsHaveNoFrameworkAnnotations() {
    return DcaRule.check(
            "DCA-ADV-004",
            "Domain events must not carry prohibited framework metadata",
            "Domain objects carry no metadata for container management, persistence or transaction coordination",
            arch -> DomainMetadata.check(arch, "DCA-ADV-004"))
        .selecting(
            "Non-interface domain events in domain packages. Metadata ownership is exclusive: events, services, factories, specifications, then domain.model types.")
        .checking(
            "Direct or meta-annotations: types prohibit injectable, persistenceEntity and transactional roles; fields prohibit injectionSite and persistenceMapping; methods prohibit transactional and eventListener, plus injectionSite except on events; constructors prohibit injectionSite. Unclassified annotations are allowed by this check. Empty configured roles select no metadata; wiring is not established.");
  }

  public DcaRule integrationEventsAreAnnotatedWithIntegrationEventType() {
    return DcaRule.of(
            "DCA-ADV-005",
            "Integration Events must be annotated with IntegrationEventType",
            "@IntegrationEventType(name, version) is the contract identity of every integration event"
                + " — the serializer keys (name, version) to the class and stamps both onto the wire"
                + " envelope",
            arch ->
                classes()
                    .that()
                    .areAssignableTo(arch.layout().markers().integrationEvent())
                    .and()
                    .areNotInterfaces()
                    .should()
                    .beAnnotatedWith(IntegrationEventType.class)
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to"
                + " IntegrationEvent - records, enums and abstract classes included.")
        .checking(
            "The class itself is annotated with @IntegrationEventType. An annotation on a supertype does not"
                + " count; the annotation's name and version values are not checked. An empty selection passes.");
  }

  public DcaRule integrationEventsHaveNoVersionField() {
    return DcaRule.check(
            "DCA-ADV-006",
            "Integration events carry the schema version in their type metadata, not in the payload",
            "The schema version is a class property (@IntegrationEventType), never per-instance payload"
                + " data — an explicit schema-version data field duplicates the annotation and can drift from it",
            arch -> {
              List<String> violations =
                  violations(
                      arch,
                      c ->
                          c.isAssignableTo(arch.layout().markers().integrationEvent())
                              && !c.isInterface(),
                      AdvancedPatternRules::hasVersionField,
                      c ->
                          c.getName()
                              + " carries an explicit schema-version field — declare the version in"
                              + " @IntegrationEventType instead");
              failIfAny(
                  violations,
                  "Integration events carry the schema version in their type metadata, not in the payload — @IntegrationEventType is the"
                      + " single source of truth:");
            })
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to"
                + " IntegrationEvent.")
        .checking(
            "Name heuristic: declared or inherited fields named schemaVersion, eventVersion or contractVersion are reported, including record components. A business revision named version is allowed, whatever its type. This does not infer a field's business meaning; an empty selection passes.");
  }

  public DcaRule domainOnlyEventsHaveNoVersionField() {
    return DcaRule.check(
            "DCA-ADV-007",
            "Domain events that are not integration events carry no schema version",
            "Versioning is a contract concern of integration events — a purely internal domain event"
                + " has no wire contract to version",
            arch -> {
              List<String> violations =
                  violations(
                      arch,
                      c ->
                          c.isAssignableTo(arch.layout().markers().domainEvent())
                              && !c.isAssignableTo(arch.layout().markers().integrationEvent())
                              && !c.isInterface(),
                      AdvancedPatternRules::hasVersionField,
                      c ->
                          c.getName()
                              + " has an explicit schema-version field but is not an IntegrationEvent — only"
                              + " IntegrationEvents need schema versioning");
              failIfAny(
                  violations,
                  "Domain Events (non-IntegrationEvent) must not have an explicit schema-version field — schema versioning is"
                      + " only for IntegrationEvents:");
            })
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to DomainEvent"
                + " but not to IntegrationEvent. A class assignable to both is not selected.")
        .checking(
            "Name heuristic: declared or inherited fields named schemaVersion, eventVersion or contractVersion are reported, including record components. A business revision named version is allowed, whatever its type. This does not infer a field's business meaning; an empty selection passes.");
  }

  public DcaRule domainEventsHaveTimestampField() {
    return DcaRule.check(
            "DCA-ADV-008",
            "Domain Events must have a timestamp field",
            "An event records something that happened — without a timestamp the fact cannot be"
                + " ordered, replayed or audited",
            arch -> {
              List<String> violations =
                  violations(
                      arch,
                      c ->
                          c.isAssignableTo(arch.layout().markers().domainEvent())
                              && !c.isInterface(),
                      c -> !hasTimestampField(c, arch.layout().timestampTypes()),
                      c -> c.getName() + " does not have a timestamp field");
              failIfAny(
                  violations,
                  "Domain Events must have a timestamp field (when did the event occur?):");
            })
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to DomainEvent."
                + " IntegrationEvent does not extend DomainEvent, so an integration event is selected only when it"
                + " also implements DomainEvent.")
        .checking(
            "At least one field - declared by the class or inherited from a supertype, static or not, of any"
                + " name - has one of the configured timestamp types, by default java.time.Instant,"
                + " OffsetDateTime, ZonedDateTime or LocalDateTime; a record component of one of these types"
                + " counts. LocalDate, long or Date fields do not satisfy it, and a timestamp method without a"
                + " backing field does not either - which is what the rule is for, because the marker already"
                + " forces the accessor. A project whose own event vocabulary wraps the timestamp in a value"
                + " object names that type with withTimestampTypes. Every offender is reported in one"
                + " violation; an empty selection passes.");
  }

  // ============================================================================
  // DOMAIN SERVICES PATTERN
  // ============================================================================

  public DcaRule domainServicesResideInDomain() {
    return DcaRule.of(
            "DCA-ADV-010",
            "Marked domain services reside in a module domain",
            "Domain services are part of the domain layer, not application layer",
            arch ->
                classes()
                    .that()
                    .areAssignableTo(arch.layout().markers().domainService())
                    .and()
                    .areNotInterfaces()
                    .should()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to"
                + " DomainService.")
        .checking(
            "Each resides in a domain package of some module root (<module>.domain..). An empty selection"
                + " passes.");
  }

  public DcaRule domainServicesHaveNoFrameworkAnnotations() {
    return DcaRule.check(
            "DCA-ADV-011",
            "Domain services must not carry prohibited framework metadata",
            "Domain objects carry no metadata for container management, persistence or transaction coordination",
            arch -> DomainMetadata.check(arch, "DCA-ADV-011"))
        .selecting(
            "Non-interface domain services in domain packages. Metadata ownership is exclusive: events, services, factories, specifications, then domain.model types.")
        .checking(
            "Direct or meta-annotations: types prohibit injectable, persistenceEntity and transactional roles; fields prohibit injectionSite and persistenceMapping; methods prohibit transactional and eventListener, plus injectionSite except on events; constructors prohibit injectionSite. Unclassified annotations are allowed by this check. Empty configured roles select no metadata; wiring is not established.");
  }

  public DcaRule domainServicesAreStateless() {
    return DcaRule.of(
            "DCA-ADV-012",
            "Domain Services should be stateless (only final fields for dependencies)",
            "Domain services should be stateless (only final fields for dependencies)",
            arch ->
                classes()
                    .that()
                    .areAssignableTo(arch.layout().markers().domainService())
                    .and()
                    .areNotInterfaces()
                    .and()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .should(haveOnlyFinalFieldsIncludingInherited())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes in <module>.domain.. of every module root that are assignable to"
                + " DomainService.")
        .checking(
            "Every field - declared or inherited from a superclass, static fields included - is final."
                + " Field types are not inspected, so a final field holding mutable state passes. An empty"
                + " selection passes.");
  }

  // ============================================================================
  // FACTORIES PATTERN
  // ============================================================================

  public DcaRule factoriesAreNamedFactory() {
    return DcaRule.of(
            "DCA-ADV-013",
            "Types carrying the factory role are named *Factory",
            "A factory is found by its role, and read by its name: a type that creates aggregates"
                + " and is not called one makes the creation site hard to find in review and in a"
                + " search",
            arch ->
                classes()
                    .that()
                    .areAssignableTo(arch.layout().markers().factory())
                    .and()
                    .areNotInterfaces()
                    .should()
                    .haveSimpleNameEndingWith(arch.layout().factorySuffix())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to Factory.")
        .checking(
            "The simple name ends with Factory. Only the suffix is checked. An empty selection passes.");
  }

  public DcaRule factoriesResideInDomain() {
    return DcaRule.of(
            "DCA-ADV-014",
            "Factories must reside in domain package",
            "Factories are part of the domain layer (complex aggregate creation logic)",
            arch ->
                classes()
                    .that()
                    .areAssignableTo(arch.layout().markers().factory())
                    .and()
                    .areNotInterfaces()
                    .should()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to Factory.")
        .checking(
            "Each resides in a domain package of some module root (<module>.domain..). An empty selection"
                + " passes.");
  }

  public DcaRule factoriesHaveNoFrameworkAnnotations() {
    return DcaRule.check(
            "DCA-ADV-015",
            "Factories must not carry prohibited framework metadata",
            "Domain objects carry no metadata for container management, persistence or transaction coordination",
            arch -> DomainMetadata.check(arch, "DCA-ADV-015"))
        .selecting(
            "Non-interface factories in domain packages. Metadata ownership is exclusive: events, services, factories, specifications, then domain.model types.")
        .checking(
            "Direct or meta-annotations: types prohibit injectable, persistenceEntity and transactional roles; fields prohibit injectionSite and persistenceMapping; methods prohibit transactional and eventListener, plus injectionSite except on events; constructors prohibit injectionSite. Unclassified annotations are allowed by this check. Empty configured roles select no metadata; wiring is not established.");
  }

  public DcaRule factoriesAreStateless() {
    return DcaRule.of(
            "DCA-ADV-016",
            "Factories should be stateless (only final fields for dependencies)",
            "Factories should be stateless (only final fields for dependencies)",
            arch ->
                classes()
                    .that()
                    .areAssignableTo(arch.layout().markers().factory())
                    .and()
                    .areNotInterfaces()
                    .and()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .should(haveOnlyFinalFieldsIncludingInherited())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes in <module>.domain.. of every module root that are assignable to"
                + " Factory.")
        .checking(
            "Every field - declared or inherited from a superclass, static fields included - is final."
                + " Field types are not inspected. An empty selection passes.");
  }

  // ============================================================================
  // SPECIFICATION PATTERN
  // ============================================================================

  public DcaRule specificationsResideInDomain() {
    return DcaRule.of(
            "DCA-ADV-017",
            "Specifications reside in the domain layer",
            "A specification is a rule of the model expressed as a predicate; it belongs where the"
                + " model is, not in the layer that happens to ask the question",
            arch ->
                classes()
                    .that(
                        specifications(
                            arch.layout().markers(), arch.layout().specificationSuffix()))
                    .should()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to the"
                + " configured specification role or whose simple name ends with Specification, the"
                + " role's own type and a class named exactly Specification excluded. The marker"
                + " and the name both select, so a specification named after the predicate it"
                + " expresses is governed too.")
        .checking(
            "Each resides in a domain package of some module root (<module>.domain..). An empty selection"
                + " passes.",
            "move the specification into the module's domain package");
  }

  public DcaRule specificationsHaveNoFrameworkAnnotations() {
    return DcaRule.check(
            "DCA-ADV-018",
            "Specifications must not carry prohibited framework metadata",
            "Domain objects carry no metadata for container management, persistence or transaction coordination",
            arch -> DomainMetadata.check(arch, "DCA-ADV-018"))
        .selecting(
            "Non-interface types in domain packages that are assignable to the configured specification role or whose simple name ends with Specification. Metadata ownership is exclusive: events, services, factories, specifications, then domain.model types.")
        .checking(
            "Direct or meta-annotations: types prohibit injectable, persistenceEntity and transactional roles; fields prohibit injectionSite and persistenceMapping; methods prohibit transactional and eventListener, plus injectionSite except on events; constructors prohibit injectionSite. Unclassified annotations are allowed by this check. Empty configured roles select no metadata; wiring is not established.");
  }

  // ============================================================================
  // HELPERS
  // ============================================================================

  /**
   * A specification: assignable to the configured role, or named after the pattern. Both select,
   * because a project may carry the marker without the suffix — as both reference samples do — or
   * the suffix without the marker.
   */
  private static DescribedPredicate<JavaClass> specifications(DcaMarkers markers, String suffix) {
    DescribedPredicate<JavaClass> byRole =
        JavaClass.Predicates.assignableTo(markers.specification());
    DescribedPredicate<JavaClass> byName = JavaClass.Predicates.simpleNameEndingWith(suffix);
    return byRole
        .or(byName)
        .and(DescribedPredicate.not(JavaClass.Predicates.INTERFACES))
        .and(DescribedPredicate.not(JavaClass.Predicates.simpleName(suffix)))
        .and(
            DescribedPredicate.not(
                DescribedPredicate.describe(
                    "the role's own type", c -> c.getName().equals(markers.specification()))))
        .as("specifications");
  }

  /**
   * Like ArchUnit's {@code haveOnlyFinalFields()}, but over {@code getAllFields()}: a mutable field
   * a domain service or factory inherits from a base class is state all the same.
   */
  private static ArchCondition<JavaClass> haveOnlyFinalFieldsIncludingInherited() {
    return new ArchCondition<>("have only final fields, inherited ones included") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        item.getAllFields().stream()
            .filter(f -> !f.getModifiers().contains(JavaModifier.FINAL))
            .filter(f -> !f.getOwner().isEquivalentTo(Object.class))
            .forEach(
                f ->
                    events.add(
                        SimpleConditionEvent.violated(
                            item,
                            item.getSimpleName()
                                + " has non-final field '"
                                + f.getName()
                                + "'"
                                + (f.getOwner().equals(item)
                                    ? ""
                                    : " inherited from " + f.getOwner().getSimpleName()))));
      }
    };
  }

  private static boolean hasVersionField(JavaClass eventClass) {
    return eventClass.getAllFields().stream().anyMatch(f -> SCHEMA_FIELDS.contains(f.getName()));
  }

  private static boolean hasTimestampField(JavaClass eventClass, List<String> timestampTypes) {
    return eventClass.getAllFields().stream()
        .anyMatch(f -> timestampTypes.contains(f.getRawType().getName()));
  }

  private static List<String> violations(
      DcaArchitecture arch,
      Predicate<JavaClass> candidate,
      Predicate<JavaClass> violates,
      java.util.function.Function<JavaClass, String> message) {
    List<String> violations = new ArrayList<>();
    for (JavaClass javaClass : arch.classes()) {
      if (candidate.test(javaClass) && violates.test(javaClass)) {
        violations.add(message.apply(javaClass));
      }
    }
    return violations;
  }

  private static void failIfAny(List<String> violations, String header) {
    if (!violations.isEmpty()) {
      throw new DcaRuleViolation(header, violations);
    }
  }
}
