package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainService;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Factory;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEventType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
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
            domainServicesResideInDomainService(),
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
                    .implement(DomainEvent.class)
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
                    .implement(DomainEvent.class)
                    .should()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to DomainEvent.")
        .checking(
            "Each resides in a domain package of some module root (<module>.domain..). An event in an"
                + " application, adapter or infrastructure package is reported. An empty selection passes.");
  }

  public DcaRule domainEventsAreImmutable() {
    return DcaRule.of(
            "DCA-ADV-003",
            "Domain Events should be immutable (final or records)",
            "Domain events should be immutable (final classes or records)",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .and()
                    .implement(DomainEvent.class)
                    .and()
                    .areNotInterfaces()
                    .and()
                    .areNotEnums()
                    .and()
                    .areNotRecords()
                    .should()
                    .haveModifier(JavaModifier.FINAL)
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface, non-enum, non-record classes in <module>.domain.. of every module root that are"
                + " assignable to DomainEvent.")
        .checking(
            "The class is final. Records and enums are not selected, so a record event always passes here."
                + " An empty selection passes.");
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
                    .areAssignableTo(IntegrationEvent.class)
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
                      c -> c.isAssignableTo(IntegrationEvent.class) && !c.isInterface(),
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
                          c.isAssignableTo(DomainEvent.class)
                              && !c.isAssignableTo(IntegrationEvent.class)
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
                      c -> c.isAssignableTo(DomainEvent.class) && !c.isInterface(),
                      c -> !hasTimestampField(c),
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
                + " name - has the raw type java.time.Instant, java.time.LocalDateTime or java.time.ZonedDateTime;"
                + " a record component of one of these types counts. OffsetDateTime, LocalDate, long or Date"
                + " fields do not satisfy it, and a timestamp method without a backing field does not either."
                + " Every offender is reported in one violation; an empty selection passes.");
  }

  // ============================================================================
  // DOMAIN SERVICES PATTERN
  // ============================================================================

  public DcaRule domainServicesResideInDomainService() {
    return DcaRule.of(
            "DCA-ADV-009",
            "Marked domain services reside in the configured domain service segment",
            "Domain services implement DomainService marker and reside in domain.service packages"
                + " (named descriptively, e.g., PricingService, CartTotalCalculator)",
            arch ->
                classes()
                    .that()
                    .implement(DomainService.class)
                    .and()
                    .areNotInterfaces()
                    .should()
                    .resideInAPackage(".." + layout.domainSubpackage() + ".service..")
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan that are assignable to"
                + " DomainService.")
        .checking(
            "Each resides in a package matching ..<domain subpackage>.service.. - the configured domain"
                + " subpackage followed by service, anywhere in the package path, not tied to a module root. A"
                + " domain service directly in domain or in domain.model is reported. An empty selection passes.");
  }

  public DcaRule domainServicesResideInDomain() {
    return DcaRule.of(
            "DCA-ADV-010",
            "Marked domain services reside in a module domain",
            "Domain services are part of the domain layer, not application layer",
            arch ->
                classes()
                    .that()
                    .implement(DomainService.class)
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
                    .implement(DomainService.class)
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
            "Factories should implement Factory Marker Interface",
            "Classes implementing Factory marker should have 'Factory' in their name",
            arch ->
                classes()
                    .that()
                    .implement(Factory.class)
                    .should()
                    .haveSimpleNameEndingWith("Factory")
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
                    .implement(Factory.class)
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
                    .implement(Factory.class)
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
            "Specifications must end with 'Specification'",
            "Specification implementations are part of the domain layer",
            arch ->
                classes()
                    .that()
                    .haveSimpleNameEndingWith("Specification")
                    .and()
                    .areNotInterfaces()
                    .and()
                    .doNotHaveSimpleName("Specification")
                    .should()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes anywhere on the classpath under scan whose simple name ends with"
                + " Specification, excluding a class named exactly Specification. No marker is involved - only"
                + " the name selects.")
        .checking(
            "Each resides in a domain package of some module root (<module>.domain..). An empty selection"
                + " passes.");
  }

  public DcaRule specificationsHaveNoFrameworkAnnotations() {
    return DcaRule.check(
            "DCA-ADV-018",
            "Specifications must not carry prohibited framework metadata",
            "Domain objects carry no metadata for container management, persistence or transaction coordination",
            arch -> DomainMetadata.check(arch, "DCA-ADV-018"))
        .selecting(
            "Non-interface specifications in domain packages. Metadata ownership is exclusive: events, services, factories, specifications, then domain.model types.")
        .checking(
            "Direct or meta-annotations: types prohibit injectable, persistenceEntity and transactional roles; fields prohibit injectionSite and persistenceMapping; methods prohibit transactional and eventListener, plus injectionSite except on events; constructors prohibit injectionSite. Unclassified annotations are allowed by this check. Empty configured roles select no metadata; wiring is not established.");
  }

  // ============================================================================
  // HELPERS
  // ============================================================================

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

  private static boolean hasTimestampField(JavaClass eventClass) {
    return eventClass.getAllFields().stream()
        .anyMatch(
            f ->
                f.getRawType().isEquivalentTo(Instant.class)
                    || f.getRawType().isEquivalentTo(LocalDateTime.class)
                    || f.getRawType().isEquivalentTo(ZonedDateTime.class));
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
