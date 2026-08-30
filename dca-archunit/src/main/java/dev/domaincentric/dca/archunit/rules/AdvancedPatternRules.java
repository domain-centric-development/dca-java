package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
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

  private static final String VERSION_FIELD = "version";

  private final DcaLayout layout;
  private final List<DcaRule> rules;

  public AdvancedPatternRules(DcaLayout layout) {
    this.layout = Objects.requireNonNull(layout, "layout");
    this.rules =
        List.of(
            domainEventsAreRecords(),
            domainEventsResideInDomain(),
            domainEventsAreImmutable(),
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
        "Domain Events must implement DomainEvent Marker Interface and be records",
        "Domain events should be immutable records implementing DomainEvent (named in past tense,"
            + " e.g., ProductCreated, CartCleared)",
        arch ->
            classes()
                .that()
                .implement(DomainEvent.class)
                .and()
                .areNotInterfaces()
                .should()
                .beRecords()
                .allowEmptyShould(true));
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
                .resideInAnyPackage(layout.domainPattern())
                .allowEmptyShould(true));
  }

  public DcaRule domainEventsAreImmutable() {
    return DcaRule.of(
        "DCA-ADV-003",
        "Domain Events should be immutable (final or records)",
        "Domain events should be immutable (final classes or records)",
        arch ->
            classes()
                .that()
                .resideInAnyPackage(layout.domainPattern())
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
                .allowEmptyShould(true));
  }

  public DcaRule domainEventsHaveNoFrameworkAnnotations() {
    return DcaRule.of(
        "DCA-ADV-004",
        "Domain Events must not have Spring annotations",
        "Domain events must be framework-independent POJOs",
        arch ->
            noClasses()
                .that()
                .resideInAnyPackage(layout.domainPattern())
                .and()
                .implement(DomainEvent.class)
                .should()
                .beAnnotatedWith(layout.frameworkAnnotations().component())
                .orShould()
                .beAnnotatedWith(layout.frameworkAnnotations().service())
                .orShould()
                .beAnnotatedWith(layout.frameworkAnnotations().eventListener())
                .allowEmptyShould(true));
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
                .allowEmptyShould(true));
  }

  public DcaRule integrationEventsHaveNoVersionField() {
    return DcaRule.check(
        "DCA-ADV-006",
        "Integration Events must not have a version field",
        "The schema version is a class property (@IntegrationEventType), never per-instance payload"
            + " data — a version data field duplicates the annotation and can drift from it",
        arch -> {
          List<String> violations =
              violations(
                  arch,
                  c -> c.isAssignableTo(IntegrationEvent.class) && !c.isInterface(),
                  AdvancedPatternRules::hasVersionField,
                  c ->
                      c.getName()
                          + " carries a version data field — declare the version in"
                          + " @IntegrationEventType instead");
          failIfAny(
              violations,
              "Integration Events must not have a version field — @IntegrationEventType is the"
                  + " single source of truth:");
        });
  }

  public DcaRule domainOnlyEventsHaveNoVersionField() {
    return DcaRule.check(
        "DCA-ADV-007",
        "Domain Events that are not Integration Events must not have a version field",
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
                          + " has a version field but is not an IntegrationEvent — only"
                          + " IntegrationEvents need versioning");
          failIfAny(
              violations,
              "Domain Events (non-IntegrationEvent) must not have a version field — versioning is"
                  + " only for IntegrationEvents:");
        });
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
              violations, "Domain Events must have a timestamp field (when did the event occur?):");
        });
  }

  // ============================================================================
  // DOMAIN SERVICES PATTERN
  // ============================================================================

  public DcaRule domainServicesResideInDomainService() {
    return DcaRule.of(
        "DCA-ADV-009",
        "Domain Services must implement DomainService Marker Interface and reside in domain.service",
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
                .allowEmptyShould(true));
  }

  public DcaRule domainServicesResideInDomain() {
    return DcaRule.of(
        "DCA-ADV-010",
        "Domain Services must reside in domain package",
        "Domain services are part of the domain layer, not application layer",
        arch ->
            classes()
                .that()
                .implement(DomainService.class)
                .should()
                .resideInAnyPackage(layout.domainPattern())
                .allowEmptyShould(true));
  }

  public DcaRule domainServicesHaveNoFrameworkAnnotations() {
    return DcaRule.of(
        "DCA-ADV-011",
        "Domain Services must not have Spring annotations",
        "Domain services should be framework-independent",
        arch ->
            noClasses()
                .that()
                .implement(DomainService.class)
                .should()
                .beAnnotatedWith(layout.frameworkAnnotations().service())
                .orShould()
                .beAnnotatedWith(layout.frameworkAnnotations().component())
                .allowEmptyShould(true));
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
                .resideInAnyPackage(layout.domainPattern())
                .should()
                .haveOnlyFinalFields()
                .allowEmptyShould(true));
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
                .allowEmptyShould(true));
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
                .resideInAnyPackage(layout.domainPattern())
                .allowEmptyShould(true));
  }

  public DcaRule factoriesHaveNoFrameworkAnnotations() {
    return DcaRule.of(
        "DCA-ADV-015",
        "Factories must not have Spring annotations",
        "Factories should be framework-independent",
        arch ->
            noClasses()
                .that()
                .implement(Factory.class)
                .and()
                .resideInAnyPackage(layout.domainPattern())
                .should()
                .beAnnotatedWith(layout.frameworkAnnotations().component())
                .orShould()
                .beAnnotatedWith(layout.frameworkAnnotations().service())
                .allowEmptyShould(true));
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
                .resideInAnyPackage(layout.domainPattern())
                .should()
                .haveOnlyFinalFields()
                .allowEmptyShould(true));
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
                .resideInAnyPackage(layout.domainPattern())
                .allowEmptyShould(true));
  }

  public DcaRule specificationsHaveNoFrameworkAnnotations() {
    return DcaRule.of(
        "DCA-ADV-018",
        "Specifications must not have Spring annotations",
        "Specifications should be framework-independent value objects",
        arch ->
            noClasses()
                .that()
                .haveSimpleNameEndingWith("Specification")
                .and()
                .resideInAnyPackage(layout.domainPattern())
                .should()
                .beAnnotatedWith(layout.frameworkAnnotations().component())
                .orShould()
                .beAnnotatedWith(layout.frameworkAnnotations().service())
                .allowEmptyShould(true));
  }

  // ============================================================================
  // HELPERS
  // ============================================================================

  private static boolean hasVersionField(JavaClass eventClass) {
    return eventClass.getAllFields().stream().anyMatch(f -> VERSION_FIELD.equals(f.getName()));
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
