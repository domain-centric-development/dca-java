package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;
import java.util.List;

/**
 * Naming conventions: consistent names for use cases, input ports, repositories, controllers, DTOs,
 * converters and view models, and ubiquitous-language names in the domain.
 */
public final class NamingRules implements DcaRuleSet {

  private final List<DcaRule> rules;

  public NamingRules(DcaLayout layout) {
    this.rules =
        List.of(
            inputPortImplementationsEndWithUseCaseSuffix(layout),
            useCasesAreServices(layout),
            inputPortInterfacesEndWithInputPort(layout),
            repositoryInterfacesEndWithRepository(layout),
            controllersEndWithController(layout),
            restControllersEndWithRestControllerSuffix(layout),
            dtosResideInAdapterLayer(layout),
            convertersResideInAdapterLayer(layout),
            noTechnicalBucketPackages(layout),
            noTechnicalSuffixesInDomain(layout),
            viewModelsResideInIncomingWebAdapters(layout));
  }

  @Override
  public String name() {
    return "naming";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  public static DcaRule inputPortImplementationsEndWithUseCaseSuffix(DcaLayout layout) {
    return DcaRule.of(
            "DCA-NAM-001",
            "Application layer InputPort implementations must end with '"
                + layout.useCaseSuffix()
                + "'",
            "InputPort implementations (use cases) should follow consistent naming conventions"
                + " (Hexagonal Architecture)",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .and()
                    .areNotInterfaces()
                    .and()
                    .areNotRecords()
                    .and()
                    .implement(UseCase.class)
                    .should()
                    .haveSimpleNameEndingWith(layout.useCaseSuffix())
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface, non-record classes in <module>.application.. of every module root that"
                + " implement UseCase.")
        .checking(
            "The simple name ends with the configured use-case suffix. Interfaces and records are"
                + " not selected; a class implementing only InputPort without UseCase is not"
                + " selected either. An empty selection passes.");
  }

  public static DcaRule useCasesAreServices(DcaLayout layout) {
    List<String> injectable = layout.frameworkAnnotations().injectable();
    return DcaRule.of(
            "DCA-NAM-002",
            "Use case classes must carry the injectable stereotype the container needs",
            "Use cases are container-managed components: the incoming adapters receive them by"
                + " injection, and the container's transaction and event plumbing only applies to"
                + " managed beans",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .and()
                    .haveSimpleNameEndingWith(layout.useCaseSuffix())
                    .and()
                    .areNotInterfaces()
                    .and(AnnotationRoles.whenConfigured(injectable))
                    .should(AnnotationRoles.beAnnotatedWithAny(injectable))
                    .allowEmptyShould(true))
        .selecting(
            "Non-interface classes in <module>.application.. of every module root whose simple name"
                + " ends with the configured use-case suffix - provided the layout configures at"
                + " least one injectable stereotype; with an empty role (a hand-wired application)"
                + " nothing is selected.")
        .checking(
            "The class is directly annotated with one of the configured injectable stereotypes."
                + " Records are selected like any other class; the marker interfaces are not"
                + " consulted - only the suffix selects. An empty selection passes.");
  }

  public static DcaRule inputPortInterfacesEndWithInputPort(DcaLayout layout) {
    // Matched by marker, not by package: DCA places each input port in its own use-case folder,
    // so there is no single ..application.port.in.. package to point at.
    return DcaRule.of(
            "DCA-NAM-003",
            "InputPort interfaces must end with 'InputPort'",
            "Input port interfaces should follow consistent naming conventions (Hexagonal Architecture)",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .and()
                    .areInterfaces()
                    .and()
                    .areAssignableTo(InputPort.class)
                    .and()
                    .doNotHaveSimpleName("InputPort")
                    .and()
                    .doNotHaveSimpleName("UseCase")
                    .should()
                    .haveSimpleNameEndingWith("InputPort")
                    .allowEmptyShould(true))
        .selecting(
            "Interfaces in <module>.application.. of every module root that are assignable to"
                + " InputPort, except those named exactly InputPort or UseCase.")
        .checking(
            "The simple name ends with InputPort. Classes and records are not selected, and an"
                + " interface extending InputPort outside an application package is not checked. An"
                + " empty selection passes.");
  }

  public static DcaRule repositoryInterfacesEndWithRepository(DcaLayout layout) {
    return DcaRule.of(
            "DCA-NAM-004",
            "Repository Interfaces must end with 'Repository'",
            "Repository interfaces should follow consistent naming conventions (DDD pattern)",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .and()
                    .areInterfaces()
                    .and()
                    .haveSimpleNameContaining("Repository")
                    .and()
                    .doNotHaveSimpleName("Repository")
                    .should()
                    .haveSimpleNameEndingWith("Repository")
                    .allowEmptyShould(true))
        .selecting(
            "Interfaces in <module>.application.. of every module root whose simple name contains"
                + " Repository, except one named exactly Repository.")
        .checking(
            "The simple name ends with Repository (RepositoryPort or ProductRepositoryAdapter is"
                + " reported). Selection is by name only - whether the interface extends the"
                + " Repository marker is not checked, and classes are not selected. An empty"
                + " selection passes.");
  }

  public static DcaRule controllersEndWithController(DcaLayout layout) {
    return DcaRule.of(
            "DCA-NAM-005",
            "Controller classes must end with '" + layout.controllerSuffix() + "'",
            "Classes carrying the web-controller stereotype should follow naming conventions",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allIncomingAdapterPatterns())
                    .and(
                        AnnotationRoles.annotatedWithAny(
                            layout.frameworkAnnotations().webController()))
                    .should()
                    .haveSimpleNameEndingWith(layout.controllerSuffix())
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.adapter.incoming.. of every module root that are directly"
                + " annotated with one of the configured web-controller stereotypes.")
        .checking(
            "The simple name ends with the configured controller suffix (default Controller). A"
                + " class carrying only a REST-controller stereotype is not selected here, and a"
                + " controller outside an incoming-adapter package is not checked. An empty"
                + " selection passes - which is always the case when the role is empty.");
  }

  public static DcaRule restControllersEndWithRestControllerSuffix(DcaLayout layout) {
    return DcaRule.of(
            "DCA-NAM-006",
            "REST Controllers must end with '"
                + layout.restControllerSuffix()
                + "' (REST best practice)",
            "Classes carrying the REST-controller stereotype should end with '"
                + layout.restControllerSuffix()
                + "' following RESTful naming conventions",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allIncomingAdapterPatterns())
                    .and(
                        AnnotationRoles.annotatedWithAny(
                            layout.frameworkAnnotations().restController()))
                    .should()
                    .haveSimpleNameEndingWith(layout.restControllerSuffix())
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.adapter.incoming.. of every module root that are directly"
                + " annotated with one of the configured REST-controller stereotypes.")
        .checking(
            "The simple name ends with the configured REST-controller suffix. Classes annotated"
                + " only with a web-controller stereotype are not selected, and a REST controller"
                + " outside an incoming-adapter package is not checked. An empty selection"
                + " passes - which is always the case when the role is empty.");
  }

  public static DcaRule dtosResideInAdapterLayer(DcaLayout layout) {
    return DcaRule.of(
            "DCA-NAM-007",
            "DTOs must reside in the adapter layer, not in domain or application",
            "DTOs are adapter concerns (presentation or external API) - not in domain or application",
            arch ->
                classes()
                    .that()
                    .haveSimpleNameEndingWith("Dto")
                    .and()
                    .resideInAnyPackage(layout.basePackage() + "..")
                    .should()
                    .resideInAnyPackage(arch.allAdapterPatterns())
                    .allowEmptyShould(true))
        .selecting("Classes under the base package whose simple name ends with Dto.")
        .checking(
            "Each resides in an adapter package of some module root (<module>.adapter..), incoming"
                + " or outgoing. A Dto in a domain, application or infrastructure package is"
                + " reported; what the class contains is not checked. An empty selection passes.");
  }

  public static DcaRule convertersResideInAdapterLayer(DcaLayout layout) {
    return DcaRule.of(
            "DCA-NAM-008",
            "Converters must reside in the adapter layer",
            "Converters/Mappers translate between layers and should be in adapters",
            arch ->
                classes()
                    .that()
                    .haveSimpleNameEndingWith("Converter")
                    .and()
                    .resideInAnyPackage(layout.basePackage() + "..")
                    .should()
                    .resideInAnyPackage(arch.allAdapterPatterns())
                    .allowEmptyShould(true))
        .selecting("Classes under the base package whose simple name ends with Converter.")
        .checking(
            "Each resides in an adapter package of some module root (<module>.adapter..), incoming"
                + " or outgoing. Only the Converter suffix is checked - a class named *Mapper or"
                + " *Assembler is not selected by this rule. An empty selection passes.");
  }

  public static DcaRule noTechnicalBucketPackages(DcaLayout layout) {
    // Top-level structure must scream business capabilities (screaming architecture).
    // Technical buckets like 'entities' or 'util' hide the domain and attract unrelated code.
    // DTOs/Converters/ViewModels have their own placement rules.
    return DcaRule.of(
            "DCA-NAM-009",
            "No technical bucket packages - package by domain concept",
            "Packages are named after domain concepts from the ubiquitous language, not technical"
                + " patterns",
            arch ->
                noClasses()
                    .that()
                    .resideInAPackage(layout.basePackage() + "..")
                    .should()
                    .resideInAnyPackage(
                        "..entities..", "..valueobjects..", "..helpers..", "..util..", "..utils..")
                    .allowEmptyShould(true))
        .selecting("Every class under the base package.")
        .checking(
            "No class resides in a package whose name contains a segment entities, valueobjects,"
                + " helpers, util or utils, at any depth. Only these five segments are checked;"
                + " other technical names such as model, service or impl are not reported. An empty"
                + " selection passes.");
  }

  public static DcaRule noTechnicalSuffixesInDomain(DcaLayout layout) {
    // Domain concepts carry ubiquitous-language names. 'Manager'/'Helper'/'Util' signal
    // a missing domain concept; 'Impl'/'Implementation' signal naming by pattern instead of by
    // specialty.
    return DcaRule.of(
            "DCA-NAM-010",
            "Domain classes must not use technical suffixes (Manager, Helper, Util, Impl, Implementation)",
            "Domain names come from the ubiquitous language - name services by their specialty, not by"
                + " technical role",
            arch ->
                noClasses()
                    .that()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .should()
                    .haveSimpleNameEndingWith("Manager")
                    .orShould()
                    .haveSimpleNameEndingWith("Helper")
                    .orShould()
                    .haveSimpleNameEndingWith("Util")
                    .orShould()
                    .haveSimpleNameEndingWith("Utils")
                    .orShould()
                    .haveSimpleNameEndingWith("Impl")
                    .orShould()
                    .haveSimpleNameEndingWith("Implementation")
                    .allowEmptyShould(true))
        .selecting("Classes in <module>.domain.. of every module root.")
        .checking(
            "No simple name ends with Manager, Helper, Util, Utils, Impl or Implementation. Only these six"
                + " suffixes are checked, only in domain packages - a *Service or *Factory in the"
                + " domain is not reported, and an Impl in an adapter package is not checked. An"
                + " empty selection passes.");
  }

  /**
   * The web-adapter packages are derived from the discovered module roots ({@code
   * root.adapter.incoming.web..}), so a grouped context and a single-context application whose base
   * package is the context are governed like a flat layout.
   */
  public static DcaRule viewModelsResideInIncomingWebAdapters(DcaLayout layout) {
    return DcaRule.of(
            "DCA-NAM-011",
            "ViewModels must reside in adapter.incoming.web packages",
            "ViewModels are presentation concerns and must reside in incoming web adapter packages",
            arch ->
                classes()
                    .that()
                    .haveSimpleNameEndingWith("ViewModel")
                    .and()
                    .resideInAnyPackage(layout.basePackage() + "..")
                    .should()
                    .resideInAnyPackage(incomingWebAdapterPatterns(arch))
                    .allowEmptyShould(true))
        .selecting("Classes under the base package whose simple name ends with ViewModel.")
        .checking(
            "Each resides in <module>.adapter.incoming.web.. of some module root - the adapter and"
                + " incoming segments are the configured ones, the web segment is fixed. A"
                + " ViewModel in a domain or application package, or in a non-web incoming adapter"
                + " such as adapter.incoming.mcp, is reported. An empty selection passes.");
  }

  private static String[] incomingWebAdapterPatterns(DcaArchitecture arch) {
    DcaLayout layout = arch.layout();
    return arch.moduleRoots().stream()
        .map(
            root ->
                root
                    + "."
                    + layout.adapterSubpackage()
                    + "."
                    + layout.incomingSubpackage()
                    + ".web..")
        .toArray(String[]::new);
  }
}
