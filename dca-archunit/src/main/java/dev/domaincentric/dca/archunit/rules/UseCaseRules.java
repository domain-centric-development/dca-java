package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import java.util.List;

/**
 * Use case and mapping patterns: the generic input-port contract, Command/Query/Result models, HTTP
 * response models, domain-event publication after saving (inside a transaction), and DTO-free inner
 * layers.
 */
public final class UseCaseRules implements DcaRuleSet {

  private final List<DcaRule> rules;

  public UseCaseRules(DcaLayout layout) {
    this.rules =
        List.of(
            baseInputPortResidesInBuildingBlocks(layout),
            commandsResideInApplication(layout),
            queriesResideInApplication(layout),
            commandsAreImmutable(layout),
            queriesAreImmutable(layout),
            resultsResideInApplication(layout),
            resultsAreImmutable(layout),
            responsesResideInIncomingAdapters(layout),
            useCasesPublishDomainEventsAfterSaving(layout),
            noDtosInDomain(layout),
            noDtosInApplication(layout),
            publishingUseCasesAreTransactional(layout));
  }

  @Override
  public String name() {
    return "usecase";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  public static DcaRule baseInputPortResidesInBuildingBlocks(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-001",
        "Base InputPort interface must be in the building-blocks port in package",
        "Base InputPort interface defines the generic contract for all use cases (Hexagonal"
            + " Architecture)",
        arch ->
            classes()
                .that()
                .areInterfaces()
                .and()
                .haveSimpleName("InputPort")
                .should()
                .resideInAPackage(DcaLayout.BUILDING_BLOCKS_PORT_IN_PACKAGE)
                .allowEmptyShould(true));
  }

  public static DcaRule commandsResideInApplication(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-002",
        "Use Case Commands must end with 'Command' and reside in application package",
        "Use case commands should be in application layer (CQRS pattern)",
        arch ->
            classes()
                .that()
                .haveSimpleNameEndingWith("Command")
                .and()
                .resideInAnyPackage(layout.basePackage() + "..")
                .should()
                .resideInAnyPackage(layout.applicationPattern())
                .allowEmptyShould(true));
  }

  public static DcaRule queriesResideInApplication(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-003",
        "Use Case Queries must end with 'Query' and reside in application package",
        "Use case queries should be in application layer (CQRS pattern)",
        arch ->
            classes()
                .that()
                .haveSimpleNameEndingWith("Query")
                .and()
                .resideInAnyPackage(layout.basePackage() + "..")
                .should()
                .resideInAnyPackage(layout.applicationPattern())
                .allowEmptyShould(true));
  }

  public static DcaRule commandsAreImmutable(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-004",
        "Use Case Commands should be immutable (final or records)",
        "Use case commands should be immutable (value objects)",
        arch -> immutableApplicationModels(layout, "Command"));
  }

  public static DcaRule queriesAreImmutable(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-005",
        "Use Case Queries should be immutable (final or records)",
        "Use case queries should be immutable (value objects)",
        arch -> immutableApplicationModels(layout, "Query"));
  }

  public static DcaRule resultsResideInApplication(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-006",
        "Use Case Result Models must end with 'Result' and reside in application package",
        "Use case result models should be in application layer. Domain Value Objects with 'Result'"
            + " in name are allowed in domain layer.",
        arch ->
            classes()
                .that()
                .haveSimpleNameEndingWith("Result")
                .and()
                .resideInAnyPackage(layout.basePackage() + "..")
                .and()
                .doNotImplement(Value.class)
                .should()
                .resideInAnyPackage(layout.applicationPattern())
                .allowEmptyShould(true));
  }

  public static DcaRule resultsAreImmutable(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-007",
        "Use Case Result Models should be immutable (final or records)",
        "Use case result models should be immutable (value objects)",
        arch -> immutableApplicationModels(layout, "Result"));
  }

  public static DcaRule responsesResideInIncomingAdapters(DcaLayout layout) {
    // Matched by pattern: every incoming adapter, in any context or none, including the shared
    // kernel's adapter where cross-cutting Response classes typically live.
    return DcaRule.of(
        "DCA-USE-008",
        "HTTP Response Models must end with 'Response' and reside in adapter incoming package",
        "HTTP response models should be in adapter incoming layer",
        arch ->
            classes()
                .that()
                .haveSimpleNameEndingWith("Response")
                .and()
                .resideInAnyPackage(layout.basePackage() + "..")
                .should()
                .resideInAPackage(layout.incomingAdapterPattern())
                .allowEmptyShould(true));
  }

  public static DcaRule useCasesPublishDomainEventsAfterSaving(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-009",
        "Use cases that save an aggregate must publish its domain events",
        "A saved aggregate must not keep its events: unpublished, they are lost, and stored on the"
            + " instance they may later be published out of context. Publishing belongs after the"
            + " save, in the use case that owns the unit of work - even when the action raised no"
            + " event",
        arch ->
            classes()
                .that()
                .resideInAPackage(layout.applicationPattern())
                .and()
                .haveSimpleNameEndingWith(layout.useCaseSuffix())
                .and()
                .areNotInterfaces()
                .should(publishAfterSaving())
                .allowEmptyShould(true));
  }

  public static DcaRule noDtosInDomain(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-010",
        "DTOs must not be used in the Domain Layer",
        "Domain layer should not depend on DTOs (presentation concerns) - Dependency Inversion"
            + " Principle",
        arch ->
            noClasses()
                .that()
                .resideInAnyPackage(layout.domainPattern())
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Dto")
                .allowEmptyShould(true));
  }

  public static DcaRule noDtosInApplication(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-011",
        "DTOs must not be used in the Application Layer",
        "Application layer should use Command/Query/Response models, not presentation DTOs (Clean"
            + " Architecture)",
        arch ->
            noClasses()
                .that()
                .resideInAnyPackage(layout.applicationPattern())
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Dto")
                .allowEmptyShould(true));
  }

  private static com.tngtech.archunit.lang.ArchRule immutableApplicationModels(
      DcaLayout layout, String suffix) {
    return classes()
        .that()
        .haveSimpleNameEndingWith(suffix)
        .and()
        .resideInAnyPackage(layout.applicationPattern())
        .and()
        .areNotInterfaces()
        .and()
        .areNotRecords()
        .should()
        .haveModifier(JavaModifier.FINAL)
        .allowEmptyShould(true);
  }

  public static DcaRule publishingUseCasesAreTransactional(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-012",
        "Use cases that publish domain events must be transactional",
        "Integration events are relayed after commit (@TransactionalEventListener,"
            + " @ApplicationModuleListener) and their publication is registered in the publishing"
            + " transaction. Without an active transaction the after-commit listeners are skipped"
            + " silently and nothing is registered: the use case succeeds, the other contexts never"
            + " hear of it. The use case that publishes owns the transaction - on the class or on the"
            + " executing method",
        arch ->
            classes()
                .that()
                .resideInAPackage(layout.applicationPattern())
                .and()
                .haveSimpleNameEndingWith(layout.useCaseSuffix())
                .and()
                .areNotInterfaces()
                .should(beTransactionalWhenPublishing(layout.frameworkAnnotations().transactional()))
                .allowEmptyShould(true));
  }

  private static ArchCondition<JavaClass> beTransactionalWhenPublishing(String transactional) {
    return new ArchCondition<>("be transactional when publishing domain events") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        boolean publishes =
            item.getMethodCallsFromSelf().stream()
                .anyMatch(call -> call.getTargetOwner().isAssignableTo(DomainEventPublisher.class));
        if (!publishes) {
          return;
        }
        boolean inTransaction =
            item.isMetaAnnotatedWith(transactional)
                || item.getMethods().stream().anyMatch(m -> m.isMetaAnnotatedWith(transactional));
        events.add(
            inTransaction
                ? SimpleConditionEvent.satisfied(
                    item, item.getSimpleName() + " publishes inside a transaction")
                : SimpleConditionEvent.violated(
                    item,
                    item.getSimpleName()
                        + " publishes domain events without @"
                        + transactional.substring(transactional.lastIndexOf('.') + 1)
                        + " - after-commit listeners are skipped"));
      }
    };
  }

  private static ArchCondition<JavaClass> publishAfterSaving() {
    return new ArchCondition<>("publish the aggregate's domain events after saving it") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        boolean savesAnAggregate =
            item.getMethodCallsFromSelf().stream()
                .anyMatch(
                    call ->
                        call.getTarget().getName().equals("save")
                            && call.getTargetOwner().isAssignableTo(Repository.class));
        if (!savesAnAggregate) {
          return;
        }
        boolean publishes =
            item.getMethodCallsFromSelf().stream()
                .anyMatch(
                    call ->
                        call.getTarget().getName().equals("publishAndClearEvents")
                            && call.getTargetOwner().isAssignableTo(DomainEventPublisher.class));
        events.add(
            publishes
                ? SimpleConditionEvent.satisfied(
                    item, item.getSimpleName() + " publishes after saving")
                : SimpleConditionEvent.violated(
                    item,
                    item.getSimpleName()
                        + " saves an aggregate without publishing its domain events"));
      }
    };
  }
}
