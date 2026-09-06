package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRuleViolation;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Entity;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.IntegrationEventPublisher;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Use case and mapping patterns: the generic input-port contract, Command/Query/Result models, HTTP
 * response models, domain-event publication after saving (inside a transaction), DTO-free inner
 * layers, one consistent use-case package depth per module (flat, or grouped by feature), and
 * results that carry values rather than aggregate roots or entities.
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
            publishingUseCasesAreTransactional(layout),
            transactionalUseCasesDoNotCallRemotePorts(layout),
            useCasePackagesUseOneDepth(layout),
            resultsMustNotExposeAggregatesOrEntities(layout));
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
                .resideInAnyPackage(arch.allApplicationPatterns())
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
                .resideInAnyPackage(arch.allApplicationPatterns())
                .allowEmptyShould(true));
  }

  public static DcaRule commandsAreImmutable(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-004",
        "Use Case Commands should be immutable (final or records)",
        "Use case commands should be immutable (value objects)",
        arch -> immutableApplicationModels(arch, "Command"));
  }

  public static DcaRule queriesAreImmutable(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-005",
        "Use Case Queries should be immutable (final or records)",
        "Use case queries should be immutable (value objects)",
        arch -> immutableApplicationModels(arch, "Query"));
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
                .resideInAnyPackage(arch.allApplicationPatterns())
                .allowEmptyShould(true));
  }

  public static DcaRule resultsAreImmutable(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-007",
        "Use Case Result Models should be immutable (final or records)",
        "Use case result models should be immutable (value objects)",
        arch -> immutableApplicationModels(arch, "Result"));
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
                .resideInAnyPackage(arch.allIncomingAdapterPatterns())
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
                .resideInAnyPackage(arch.allApplicationPatterns())
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
                .resideInAnyPackage(arch.allDomainPatterns())
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
                .resideInAnyPackage(arch.allApplicationPatterns())
                .should()
                .dependOnClassesThat()
                .haveSimpleNameEndingWith("Dto")
                .allowEmptyShould(true));
  }

  private static com.tngtech.archunit.lang.ArchRule immutableApplicationModels(
      DcaArchitecture arch, String suffix) {
    return classes()
        .that()
        .haveSimpleNameEndingWith(suffix)
        .and()
        .resideInAnyPackage(arch.allApplicationPatterns())
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
        "Use cases that publish domain events must have a transaction boundary",
        "Integration events are relayed after commit (@TransactionalEventListener,"
            + " @ApplicationModuleListener) and their publication is registered in the publishing"
            + " transaction. Without an active transaction the after-commit listeners are skipped"
            + " silently and nothing is registered: the use case succeeds, the other contexts never"
            + " hear of it. The use case that publishes owns the boundary - either declarative"
            + " transaction metadata (@Transactional on the class or the executing method) or an"
            + " explicit TransactionBoundary.inTransaction(...) around save and publish",
        arch ->
            classes()
                .that()
                .resideInAnyPackage(arch.allApplicationPatterns())
                .and()
                .haveSimpleNameEndingWith(layout.useCaseSuffix())
                .and()
                .areNotInterfaces()
                .should(
                    beTransactionalWhenPublishing(layout.frameworkAnnotations().transactional()))
                .allowEmptyShould(true));
  }

  public static DcaRule transactionalUseCasesDoNotCallRemotePorts(DcaLayout layout) {
    return DcaRule.of(
        "DCA-USE-013",
        "Declaratively transactional use cases must not call remote-capable output ports",
        "A @Transactional use case holds a database connection for its whole run. Calling an"
            + " output port that may leave the process (another context's API, a payment provider,"
            + " a mail gateway) inside it blocks that connection for the remote round trip; under"
            + " load the pool runs dry, and a rollback cannot undo the remote effect. Only"
            + " transactional resources belong inside the boundary: Repository, Store,"
            + " DomainEventPublisher, IntegrationEventPublisher. Everything else is called before"
            + " the transaction - draw the boundary by hand with TransactionBoundary.inTransaction(...)"
            + " - or after it, as a reaction to an integration event",
        arch ->
            classes()
                .that()
                .resideInAnyPackage(arch.allApplicationPatterns())
                .and()
                .haveSimpleNameEndingWith(layout.useCaseSuffix())
                .and()
                .areNotInterfaces()
                .should(
                    notCallRemotePortsWhenTransactional(
                        layout.frameworkAnnotations().transactional()))
                .allowEmptyShould(true));
  }

  /**
   * Use cases live at one of two depths below a module's application package: {@code
   * application.<usecase>} (flat) or {@code application.<feature>.<usecase>} (grouped). Selects the
   * concrete classes ending in the configured use-case suffix, ignores {@code application.shared},
   * abstract classes (a shared base class is not a use case) and nested types, and reports every
   * offending module and package in one violation: a use case directly in the application package,
   * one nested deeper than a feature, or a module that mixes both forms.
   */
  public static DcaRule useCasePackagesUseOneDepth(DcaLayout layout) {
    return DcaRule.check(
        "DCA-USE-014",
        "Use case packages within a module must use one consistent depth (flat or grouped by"
            + " feature)",
        "A use case package sits either directly below the application package"
            + " (application.<usecase>) or one level deeper inside a feature"
            + " (application.<feature>.<usecase>). A feature is an optional, domain-named group of"
            + " related use cases - a navigation boundary inside one bounded context, not a layer,"
            + " module or aggregate owner. Mixing both forms in one module makes it unclear whether"
            + " a package is a feature, a use case or a leftover; nesting deeper than a feature hides"
            + " the use case. The rule checks legibility only: it does not infer bounded contexts,"
            + " feature semantics or aggregate ownership. application.shared holds the context-wide"
            + " output ports and is not a use case package",
        arch -> checkUseCaseDepth(arch, layout));
  }

  private static void checkUseCaseDepth(DcaArchitecture arch, DcaLayout layout) {
    List<String> violations = new ArrayList<>();
    for (String root : arch.moduleRoots()) {
      String application = root + "." + layout.applicationSubpackage();
      String shared = application + ".shared";
      // depth -> use case packages at that depth, both sorted for a stable message
      Map<Integer, TreeSet<String>> byDepth = new TreeMap<>();
      for (JavaClass candidate : arch.classes()) {
        String pkg = candidate.getPackageName();
        if (!(pkg.equals(application) || pkg.startsWith(application + "."))
            || pkg.equals(shared)
            || pkg.startsWith(shared + ".")
            || candidate.isInterface()
            || candidate.getModifiers().contains(JavaModifier.ABSTRACT)
            || candidate.isNestedClass()
            || candidate.isAnonymousClass()
            || !candidate.getSimpleName().endsWith(layout.useCaseSuffix())) {
          continue;
        }
        int depth =
            pkg.equals(application)
                ? 0
                : pkg.substring(application.length() + 1).split("\\.").length;
        byDepth.computeIfAbsent(depth, d -> new TreeSet<>()).add(pkg);
      }
      if (byDepth.isEmpty()) {
        continue;
      }
      byDepth
          .getOrDefault(0, new TreeSet<>())
          .forEach(
              pkg ->
                  violations.add(
                      "Module "
                          + root
                          + ": use case directly in the application package "
                          + pkg
                          + " - give it a package of its own (application.<usecase>)"));
      byDepth.forEach(
          (depth, pkgs) -> {
            if (depth > 2) {
              pkgs.forEach(
                  pkg ->
                      violations.add(
                          "Module "
                              + root
                              + ": use case package "
                              + pkg
                              + " is nested deeper than application.<feature>.<usecase>"));
            }
          });
      if (byDepth.containsKey(1) && byDepth.containsKey(2)) {
        violations.add(
            "Module "
                + root
                + " mixes flat use case packages "
                + byDepth.get(1)
                + " with feature-grouped ones "
                + byDepth.get(2)
                + " - finish the migration in one direction");
      }
    }
    if (!violations.isEmpty()) {
      throw new DcaRuleViolation(
          "Use case packages within a module must use one consistent depth (flat or grouped by"
              + " feature)",
          violations);
    }
  }

  /**
   * A result is the use case's answer: it may carry primitives, nested records, value objects,
   * enriched models and read models, never a class assignable to {@link AggregateRoot} or {@link
   * Entity}. Selects the top-level classes ending in {@code Result} below an application package
   * and walks their fields transitively: through the raw type and every generic type argument of
   * each field, and into every record that lives in an application package (part records - nested
   * in the result, next to it, or shared in {@code application.shared}), which do not carry the
   * {@code Result} suffix themselves. Records from the domain are values by contract and are not
   * walked. Reports every offending path in one violation.
   */
  public static DcaRule resultsMustNotExposeAggregatesOrEntities(DcaLayout layout) {
    return DcaRule.check(
        "DCA-USE-015",
        "Use Case Result Models must not expose aggregate roots or entities",
        "A result is the use case's answer, not a handle on the model: identity and behaviour stay"
            + " behind the port; values, enriched models and read models may cross. Checked"
            + " transitively through nested records, part records anywhere in the application layer"
            + " (application.shared included) and generic type arguments (List<T>, Optional<T>,"
            + " Map<K,V>)",
        arch -> checkResultsCarryNoIdentities(arch));
  }

  private static void checkResultsCarryNoIdentities(DcaArchitecture arch) {
    List<String> violations = new ArrayList<>();
    for (JavaClass result : arch.classes()) {
      if (result.isInterface()
          || result.isNestedClass()
          || !result.getSimpleName().endsWith("Result")
          || !residesInAny(result, arch.allApplicationPatterns())) {
        continue;
      }
      walkResult(
          arch.allApplicationPatterns(),
          result,
          result.getSimpleName(),
          new HashSet<>(),
          violations);
    }
    if (!violations.isEmpty()) {
      throw new DcaRuleViolation(
          "Use Case Result Models must not expose aggregate roots or entities", violations);
    }
  }

  private static boolean residesInAny(JavaClass javaClass, String[] packagePatterns) {
    for (String pattern : packagePatterns) {
      if (JavaClass.Predicates.resideInAPackage(pattern).test(javaClass)) {
        return true;
      }
    }
    return false;
  }

  private static void walkResult(
      String[] applicationPatterns,
      JavaClass current,
      String path,
      Set<String> visited,
      List<String> violations) {
    if (!visited.add(current.getName())) {
      return;
    }
    for (JavaField field : current.getFields()) {
      if (field.getModifiers().contains(JavaModifier.STATIC)) {
        continue;
      }
      String fieldPath = path + "." + field.getName();
      for (JavaClass involved : field.getType().getAllInvolvedRawTypes()) {
        String identity = identityKind(involved);
        if (identity != null) {
          violations.add(fieldPath + " : " + involved.getSimpleName() + " (" + identity + ")");
        } else if (isPartRecord(involved, applicationPatterns)) {
          walkResult(
              applicationPatterns,
              involved,
              fieldPath + " -> " + involved.getSimpleName(),
              visited,
              violations);
        }
      }
    }
  }

  /** The marker a class carries into the result, or null when it is a value or plain type. */
  private static String identityKind(JavaClass javaClass) {
    if (javaClass.isAssignableTo(AggregateRoot.class)) {
      return "AggregateRoot";
    }
    if (javaClass.isAssignableTo(Entity.class)) {
      return "Entity";
    }
    return null;
  }

  /**
   * A part record: a record that lives in an application package - nested in the result, declared
   * next to it, or shared in {@code application.shared}. Records from other layers (value objects,
   * read models) are values by contract and are not walked.
   */
  private static boolean isPartRecord(JavaClass candidate, String[] applicationPatterns) {
    return candidate.isRecord() && residesInAny(candidate, applicationPatterns);
  }

  private static boolean isTransactional(JavaClass item, String transactional) {
    return item.isMetaAnnotatedWith(transactional)
        || item.getMethods().stream().anyMatch(m -> m.isMetaAnnotatedWith(transactional));
  }

  private static boolean usesTransactionBoundary(JavaClass item) {
    return item.getMethodCallsFromSelf().stream()
        .anyMatch(call -> call.getTargetOwner().isAssignableTo(TransactionBoundary.class));
  }

  /**
   * Output ports that live inside the transaction; every other output port may leave the process.
   */
  private static boolean isTransactionalResource(JavaClass owner) {
    return owner.isAssignableTo(Repository.class)
        || owner.isAssignableTo(Store.class)
        || owner.isAssignableTo(DomainEventPublisher.class)
        || owner.isAssignableTo(IntegrationEventPublisher.class);
  }

  private static ArchCondition<JavaClass> notCallRemotePortsWhenTransactional(
      String transactional) {
    return new ArchCondition<>("not call remote-capable output ports while transactional") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        if (!isTransactional(item, transactional)) {
          return;
        }
        List<String> remotePorts =
            item.getMethodCallsFromSelf().stream()
                .map(call -> call.getTargetOwner())
                .filter(owner -> owner.isAssignableTo(OutputPort.class))
                .filter(owner -> !isTransactionalResource(owner))
                .map(JavaClass::getSimpleName)
                .distinct()
                .sorted()
                .toList();
        if (!remotePorts.isEmpty()) {
          events.add(
              SimpleConditionEvent.violated(
                  item,
                  item.getSimpleName()
                      + " is @"
                      + transactional.substring(transactional.lastIndexOf('.') + 1)
                      + " and calls "
                      + String.join(", ", remotePorts)
                      + " inside the transaction - call it before, or draw the boundary with"
                      + " TransactionBoundary.inTransaction(...)"));
        }
      }
    };
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
            isTransactional(item, transactional) || usesTransactionBoundary(item);
        events.add(
            inTransaction
                ? SimpleConditionEvent.satisfied(
                    item, item.getSimpleName() + " publishes inside a transaction")
                : SimpleConditionEvent.violated(
                    item,
                    item.getSimpleName()
                        + " publishes domain events without @"
                        + transactional.substring(transactional.lastIndexOf('.') + 1)
                        + " and without TransactionBoundary.inTransaction(...) - after-commit listeners are skipped"));
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
