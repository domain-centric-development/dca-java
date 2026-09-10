package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
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
import dev.domaincentric.dca.archunit.FrameworkAnnotations;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Entity;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.IntegrationEventPublisher;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

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
            resultsMustNotExposeAggregatesOrEntities(layout),
            useCasesMustNotInvokeOtherUseCases(layout),
            useCasesExposeOnlyInputPort(layout));
  }

  public static DcaRule useCasesMustNotInvokeOtherUseCases(DcaLayout layout) {
    return DcaRule.check(
            "DCA-USE-016",
            "Use cases must not invoke other use cases",
            "Ordinary operations do not coordinate other operations; coordination requires explicit transaction and failure semantics",
            OperationPolicy::invocation)
        .selecting(
            "Concrete non-nested application classes selected by InputPort marker or configured use-case suffix.")
        .checking(
            "Reports dependencies on another input port or operation, directly or transitively through same-module application helpers. Own interfaces and base classes are excluded. Messages start Caller -> Target [via Helper], so an anchored caller-side ignore permits configured coordinators without permitting calls to them. Reflection, container lookups and unrelated interfaces are not resolved; CYC-005 still checks coordinator cycles.");
  }

  public static DcaRule useCasesExposeOnlyInputPort(DcaLayout layout) {
    return DcaRule.check(
            "DCA-USE-017",
            "Use cases expose no public operation outside their input port",
            "The input port describes the complete externally callable operation surface",
            OperationPolicy::surface)
        .selecting(
            "Concrete non-nested application operations selected by InputPort marker or configured use-case suffix, with loadable runtime classes.")
        .checking(
            "Every effective public instance method, declared or inherited, matches a method of an implemented InputPort interface after generic substitution. Unrelated-interface methods and getters/setters are not exempt. A use case selected by suffix only, without an InputPort interface, has no permitted operation and is reported in full - implement the input port. Constructors, Object signatures, static and compiler-generated bridge/synthetic methods are excluded.");
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
            "The base InputPort contract is not redeclared in the project",
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
                    .allowEmptyShould(true))
        .selecting("Interfaces named InputPort anywhere on the classpath under scan.")
        .checking(
            "The interface resides in the building-blocks package hexagonal.port.in - the generic contract is not redeclared in the project.");
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
                    .allowEmptyShould(true))
        .selecting("Classes under the base package whose simple name ends with Command.")
        .checking(
            "Each resides in an application package of some module root (<module>.application..). A Command in a domain, adapter or infrastructure package is reported.");
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
                    .allowEmptyShould(true))
        .selecting("Classes under the base package whose simple name ends with Query.")
        .checking(
            "Each resides in an application package of some module root (<module>.application..).");
  }

  public static DcaRule commandsAreImmutable(DcaLayout layout) {
    return DcaRule.of(
            "DCA-USE-004",
            "Use Case Commands should be immutable (final or records)",
            "Use case commands should be immutable (value objects)",
            arch -> immutableApplicationModels(arch, "Command"))
        .selecting(
            "Non-interface classes, including records, in <module>.application.. whose simple name ends with Command.")
        .checking(
            "The type is final or a record with final inherited instance fields and no instance setter methods - a name heuristic: set followed by an upper-case letter, with parameters, returning void (settle(x) is not a setter). Referenced objects and collection contents are not inspected.");
  }

  public static DcaRule queriesAreImmutable(DcaLayout layout) {
    return DcaRule.of(
            "DCA-USE-005",
            "Use Case Queries should be immutable (final or records)",
            "Use case queries should be immutable (value objects)",
            arch -> immutableApplicationModels(arch, "Query"))
        .selecting(
            "Non-interface classes, including records, in <module>.application.. whose simple name ends with Query.")
        .checking(
            "The type is final or a record with final inherited instance fields and no instance setter methods - a name heuristic: set followed by an upper-case letter, with parameters, returning void (settle(x) is not a setter). Referenced objects and collection contents are not inspected.");
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
                    .allowEmptyShould(true))
        .selecting(
            "Classes under the base package whose simple name ends with Result and that do not implement Value.")
        .checking(
            "Each resides in an application package of some module root (<module>.application..). A domain value object named *Result is exempt because it implements Value.");
  }

  public static DcaRule resultsAreImmutable(DcaLayout layout) {
    return DcaRule.of(
            "DCA-USE-007",
            "Use Case Result Models should be immutable (final or records)",
            "Use case result models should be immutable (value objects)",
            arch -> immutableApplicationModels(arch, "Result"))
        .selecting(
            "Non-interface classes, including records, in <module>.application.. whose simple name ends with Result.")
        .checking(
            "The type is final or a record with final inherited instance fields and no instance setter methods - a name heuristic: set followed by an upper-case letter, with parameters, returning void (settle(x) is not a setter). Referenced objects and collection contents are not inspected.");
  }

  public static DcaRule responsesResideInIncomingAdapters(DcaLayout layout) {
    // Matched by pattern: every incoming adapter, in any context or none, including the shared
    // kernel's adapter where cross-cutting Response classes typically live.
    return DcaRule.of(
            "DCA-USE-008",
            "HTTP Response Models must end with 'Response' and reside in adapter package",
            "HTTP response models should be in adapter layer",
            arch ->
                classes()
                    .that()
                    .haveSimpleNameEndingWith("Response")
                    .and()
                    .resideInAnyPackage(layout.basePackage() + "..")
                    .should()
                    .resideInAnyPackage(arch.allAdapterPatterns())
                    .allowEmptyShould(true))
        .selecting("Classes under the base package whose simple name ends with Response.")
        .checking(
            "Each resides in an adapter package of some module root (<module>.adapter..), the shared kernel's included.");
  }

  public static DcaRule useCasesPublishDomainEventsAfterSaving(DcaLayout layout) {
    return DcaRule.of(
            "DCA-USE-009",
            "Use cases that save an aggregate must publish its domain events",
            "A saved aggregate must not keep its events: unpublished, they are lost, and stored on the"
                + " instance they may later be published out of context. Publishing belongs after the"
                + " save, in the use case that owns the unit of work - unless the aggregate is proven never to register an"
                + " event: its whole hierarchy is under scan and no code unit of it, of a helper it calls, or of any other"
                + " scanned class registering on that aggregate (a nested class it never calls) calls registerEvent;"
                + " a helper in another top-level class cannot reach the protected method; an unresolved type argument keeps the"
                + " requirement. Checked per entry path, following calls within the use case class: every"
                + " entry point that reaches a save - a method callable from outside the class, or one"
                + " nothing in the class calls - must also reach a publication; a wrapper that publishes"
                + " does not cover a direct call of the public method it wraps, and a helper two methods"
                + " share does not connect them. That the"
                + " publication follows the save and concerns the same aggregate is not established"
                + " statically. Only DomainEventPublisher.publishAndClearEvents counts as a publication:"
                + " iterating domainEvents() and calling publish(event), even followed by"
                + " clearDomainEvents(), separates dispatch from acknowledgement and is not accepted",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .and()
                    .haveSimpleNameEndingWith(layout.useCaseSuffix())
                    .or()
                    .areAssignableTo(InputPort.class)
                    .and()
                    .areNotInterfaces()
                    .should(publishAfterSaving(arch))
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes in <module>.application.. that implement InputPort or whose simple name ends with the configured use-case suffix.")
        .checking(
            "Only a resolved Repository<T,ID> whose aggregate and every non-building-block superclass are scanned and have no registration call (including helpers) is exempt. Unresolved generics, partial scans or undecidable external helpers remain required. For every non-exempt method of the class that calls Repository.save, every entry point reaching it (a method callable from outside the class, or one nothing in the class calls) also reaches, through calls within the class, a call of DomainEventPublisher.publishAndClearEvents. Only publishAndClearEvents counts - publish(event), even followed by clearDomainEvents(), does not. A use case without a save (a query, a bulk delete) is selected but has nothing to check and passes.");
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
                    .allowEmptyShould(true))
        .selecting("Classes in <module>.domain.. of every module root.")
        .checking("No dependency on a class whose simple name ends with Dto.");
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
                    .allowEmptyShould(true))
        .selecting("Classes in <module>.application.. of every module root.")
        .checking(
            "No dependency on a class whose simple name ends with Dto. Command, Query and Result models are not DTOs by this rule's definition - only the Dto suffix is checked.");
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
        .should(TypeInspection.haveImmutableShape())
        .allowEmptyShould(true);
  }

  public static DcaRule publishingUseCasesAreTransactional(DcaLayout layout) {
    return DcaRule.of(
            "DCA-USE-012",
            "Use cases that publish domain events must have a transaction boundary",
            "Integration events are relayed after commit by the framework's after-commit listeners,"
                + " and their publication is registered in the publishing transaction. Without an"
                + " active transaction the after-commit listeners are skipped silently and nothing is"
                + " registered: the use case succeeds, the other contexts never hear of it. The use"
                + " case that publishes owns the boundary - either declarative transaction metadata"
                + " (the configured transactional annotation on the class or the executing method) or"
                + " an explicit TransactionBoundary.inTransaction(...) around save and publish. Checked"
                + " per entry path, following calls within the class: from every entry point - a"
                + " method callable from outside the class, or one nothing in the class calls - no route"
                + " down to the publishing method may be free of an annotation or a boundary; a covered"
                + " caller does not cover another route to the same helper, and a boundary on one route"
                + " does not cover a second route. Whether the publication sits inside the block"
                + " handed to inTransaction(...) is not visible in ArchUnit's call model, which folds a"
                + " lambda's body into the enclosing method; that placement stays a review check",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .and()
                    .haveSimpleNameEndingWith(layout.useCaseSuffix())
                    .or()
                    .areAssignableTo(InputPort.class)
                    .and()
                    .areNotInterfaces()
                    .should(
                        beTransactionalWhenPublishing(
                            layout.frameworkAnnotations().transactional()))
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes in <module>.application.. that implement InputPort or whose simple name ends with the configured use-case suffix.")
        .checking(
            "For every method that calls a DomainEventPublisher, every route from each entry point"
                + " down to it is covered: the class carries one of the configured transactional"
                + " annotations, or every uncovered unit on the route is either annotated or calls"
                + " TransactionBoundary.inTransaction. A covered caller does not cover a second route"
                + " to the same helper. With an empty transactional role only the explicit boundary"
                + " counts. Whether the publish call sits inside the inTransaction block is not"
                + " checked - ArchUnit folds a lambda into its enclosing method.");
  }

  public static DcaRule transactionalUseCasesDoNotCallRemotePorts(DcaLayout layout) {
    return DcaRule.of(
            "DCA-USE-013",
            "Declaratively transactional use cases must not call remote-capable output ports",
            "A declaratively transactional use case holds a database connection for its whole run."
                + " Calling an"
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
                    .or()
                    .areAssignableTo(InputPort.class)
                    .and()
                    .areNotInterfaces()
                    .should(
                        notCallRemotePortsWhenTransactional(
                            layout.frameworkAnnotations().transactional()))
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes in <module>.application.. that implement InputPort or whose simple name ends with the configured use-case suffix.")
        .checking(
            "Every method that runs under one of the configured transactional annotations - on the"
                + " class, on itself, or on a method that reaches it within the class - calls no"
                + " OutputPort other than Repository, Store, DomainEventPublisher or"
                + " IntegrationEventPublisher. A use case without such an annotation (explicit"
                + " TransactionBoundary or none) is selected but never reported; with an empty role"
                + " nothing is ever reported.");
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
            arch -> checkUseCaseDepth(arch, layout))
        .selecting(
            "Per module root: non-interface, non-abstract, non-nested classes below <module>.application that implement InputPort or whose simple name ends with the configured use-case suffix, excluding application.shared and everything below it.")
        .checking(
            "After removing configured operationContainers segments, all of them sit at one depth: application.<usecase> (flat) or application.<feature>.<usecase> (grouped). Reported are a use case directly in the application package, one nested deeper than a feature, and a module mixing both depths. What a feature means is not checked.");
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
            || !(candidate.isAssignableTo(InputPort.class)
                || candidate.getSimpleName().endsWith(layout.useCaseSuffix()))) {
          continue;
        }
        int depth =
            pkg.equals(application)
                ? 0
                : (int)
                    java.util.Arrays.stream(pkg.substring(application.length() + 1).split("\\."))
                        .filter(segment -> !layout.operationContainers().contains(segment))
                        .count();
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
                + " (application.shared included), generic type arguments (List<T>, Optional<T>,"
                + " Map<K,V>) and inherited fields, a generic base class's type parameters resolved as"
                + " the result binds them",
            arch -> checkResultsCarryNoIdentities(arch))
        .selecting(
            "Non-interface, non-nested classes in <module>.application.. whose simple name ends with Result.")
        .checking(
            "No instance field - inherited ones included, walked through raw type and generic type arguments, and transitively into every record that lives in an application package - involves a type assignable to AggregateRoot or Entity. Records outside the application layer (domain value objects, read models) are not walked. Every offending path is reported.");
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
          new ArrayDeque<>(),
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

  /**
   * Walks the instance fields of a result or part record — inherited ones included, a base class
   * need not carry the suffix, and a generic base's type parameters are read as the result binds
   * them — and every type each field involves. The path of records currently being walked guards
   * against a self-referencing part record; it is not a global visited set, so the same part record
   * reached through two fields is reported on both paths.
   */
  private static void walkResult(
      String[] applicationPatterns,
      JavaClass current,
      String path,
      Deque<String> recordsOnPath,
      List<String> violations) {
    if (recordsOnPath.contains(current.getName())) {
      return;
    }
    recordsOnPath.push(current.getName());
    for (JavaField field : TypeInspection.instanceFields(current)) {
      String fieldPath = path + "." + field.getName();
      for (JavaClass involved : TypeInspection.involvedTypes(field, current)) {
        String identity = identityKind(involved);
        if (identity != null) {
          violations.add(fieldPath + " : " + involved.getSimpleName() + " (" + identity + ")");
        } else if (isPartRecord(involved, applicationPatterns)) {
          walkResult(
              applicationPatterns,
              involved,
              fieldPath + " -> " + involved.getSimpleName(),
              recordsOnPath,
              violations);
        }
      }
    }
    recordsOnPath.pop();
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

  private static boolean callsBoundary(JavaCodeUnit unit) {
    return unit.getMethodCallsFromSelf().stream()
        .anyMatch(call -> call.getTargetOwner().isAssignableTo(TransactionBoundary.class));
  }

  private static boolean calls(JavaCodeUnit unit, Class<?> targetType) {
    return unit.getMethodCallsFromSelf().stream()
        .anyMatch(call -> call.getTargetOwner().isAssignableTo(targetType));
  }

  private static boolean calls(JavaCodeUnit unit, Class<?> targetType, String methodName) {
    return unit.getMethodCallsFromSelf().stream()
        .anyMatch(
            call ->
                call.getTarget().getName().equals(methodName)
                    && call.getTargetOwner().isAssignableTo(targetType));
  }

  /**
   * Whether the unit may run inside declared transaction metadata: the class is annotated, the unit
   * is, or a unit that reaches it through calls within the class is. Used where one covered path is
   * enough to matter (a remote call inside a transaction).
   */
  private static boolean isTransactional(
      JavaClass item, JavaCodeUnit unit, IntraClassCalls calls, List<String> transactional) {
    return AnnotationRoles.isMetaAnnotatedWithAny(item, transactional)
        || calls.callersOf(unit).stream()
            .anyMatch(u -> AnnotationRoles.isMetaAnnotatedWithAny(u, transactional));
  }

  /**
   * Whether every route from {@code entry} down to {@code publisher} is covered: the class is
   * annotated, or no route reaches the publisher through units none of which carries the annotation
   * or draws an explicit boundary. A boundary on one route does not cover another route to the same
   * publisher.
   */
  private static boolean pathIsTransactional(
      JavaClass item,
      JavaCodeUnit entry,
      JavaCodeUnit publisher,
      IntraClassCalls calls,
      List<String> transactional) {
    if (AnnotationRoles.isMetaAnnotatedWithAny(item, transactional)) {
      return true;
    }
    Predicate<JavaCodeUnit> uncovered =
        unit ->
            !AnnotationRoles.isMetaAnnotatedWithAny(unit, transactional) && !callsBoundary(unit);
    return !calls.reachableThrough(entry, uncovered).contains(publisher);
  }

  /** {@code execute} for the unit itself, {@code execute (via persist)} when reached through it. */
  private static String pathName(JavaCodeUnit entry, JavaCodeUnit unit) {
    return entry.equals(unit) ? entry.getName() : entry.getName() + " (via " + unit.getName() + ")";
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
      List<String> transactional) {
    return new ArchCondition<>("not call remote-capable output ports while transactional") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        IntraClassCalls calls = new IntraClassCalls(item);
        for (JavaCodeUnit unit : item.getCodeUnits()) {
          if (!isTransactional(item, unit, calls, transactional)) {
            continue;
          }
          List<String> remotePorts =
              unit.getMethodCallsFromSelf().stream()
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
                        + "."
                        + unit.getName()
                        + " runs under "
                        + FrameworkAnnotations.describe(transactional, "a transaction annotation")
                        + " and calls "
                        + String.join(", ", remotePorts)
                        + " inside the transaction - call it before, or draw the boundary with"
                        + " TransactionBoundary.inTransaction(...)"));
          }
        }
      }
    };
  }

  private static ArchCondition<JavaClass> beTransactionalWhenPublishing(
      List<String> transactional) {
    return new ArchCondition<>("be transactional when publishing domain events") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        IntraClassCalls calls = new IntraClassCalls(item);
        for (JavaCodeUnit unit : item.getCodeUnits()) {
          if (!calls(unit, DomainEventPublisher.class)) {
            continue;
          }
          for (JavaCodeUnit entry : calls.entryPointsOf(unit)) {
            if (pathIsTransactional(item, entry, unit, calls, transactional)) {
              continue;
            }
            events.add(
                SimpleConditionEvent.violated(
                    item,
                    item.getSimpleName()
                        + "."
                        + pathName(entry, unit)
                        + " publishes domain events without "
                        + FrameworkAnnotations.describe(
                            transactional, "declarative transaction metadata (none configured)")
                        + " on the class or on a method of that path, and without"
                        + " TransactionBoundary.inTransaction(...) on it - after-commit"
                        + " listeners are skipped"));
          }
        }
      }
    };
  }

  private static ArchCondition<JavaClass> publishAfterSaving(DcaArchitecture arch) {
    return new ArchCondition<>("publish the aggregate's domain events after saving it") {
      @Override
      public void check(JavaClass item, ConditionEvents events) {
        IntraClassCalls calls = new IntraClassCalls(item);
        for (JavaCodeUnit unit : item.getCodeUnits()) {
          if (unit.getMethodCallsFromSelf().stream()
              .filter(
                  c ->
                      c.getTarget().getName().equals("save")
                          && c.getTargetOwner().isAssignableTo(Repository.class))
              .allMatch(c -> EventFreeAggregate.repository(c.getTargetOwner(), arch))) {
            continue;
          }
          for (JavaCodeUnit entry : calls.entryPointsOf(unit)) {
            boolean publishes =
                calls.reachableFrom(entry).stream()
                    .anyMatch(u -> calls(u, DomainEventPublisher.class, "publishAndClearEvents"));
            if (!publishes) {
              events.add(
                  SimpleConditionEvent.violated(
                      item,
                      item.getName()
                          + "."
                          + pathName(entry, unit)
                          + " saves an aggregate without publishing its domain events - no"
                          + " method reached from there calls publishAndClearEvents"));
            }
          }
        }
      }
    };
  }
}
