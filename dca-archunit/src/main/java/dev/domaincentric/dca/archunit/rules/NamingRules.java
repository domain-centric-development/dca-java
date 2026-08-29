package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
                .resideInAPackage(layout.applicationPattern())
                .and()
                .areNotInterfaces()
                .and()
                .areNotRecords()
                .and()
                .implement(UseCase.class)
                .should()
                .haveSimpleNameEndingWith(layout.useCaseSuffix())
                .allowEmptyShould(true));
  }

  public static DcaRule useCasesAreServices(DcaLayout layout) {
    return DcaRule.of(
        "DCA-NAM-002",
        "Use case classes must be annotated with @Service",
        "Use case classes must be Spring-managed beans",
        arch ->
            classes()
                .that()
                .resideInAPackage(layout.applicationPattern())
                .and()
                .haveSimpleNameEndingWith(layout.useCaseSuffix())
                .and()
                .areNotInterfaces()
                .should()
                .beAnnotatedWith(layout.frameworkAnnotations().service())
                .allowEmptyShould(true));
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
                .resideInAPackage(layout.applicationPattern())
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
                .allowEmptyShould(true));
  }

  public static DcaRule repositoryInterfacesEndWithRepository(DcaLayout layout) {
    return DcaRule.of(
        "DCA-NAM-004",
        "Repository Interfaces must end with 'Repository'",
        "Repository interfaces should follow consistent naming conventions (DDD pattern)",
        arch ->
            classes()
                .that()
                .resideInAPackage(layout.applicationPattern())
                .and()
                .areInterfaces()
                .and()
                .haveSimpleNameContaining("Repository")
                .and()
                .doNotHaveSimpleName("Repository")
                .should()
                .haveSimpleNameEndingWith("Repository")
                .allowEmptyShould(true));
  }

  public static DcaRule controllersEndWithController(DcaLayout layout) {
    return DcaRule.of(
        "DCA-NAM-005",
        "Controller classes must end with 'Controller'",
        "@Controller annotated classes should follow naming conventions",
        arch ->
            classes()
                .that()
                .resideInAPackage(layout.incomingAdapterPattern())
                .and()
                .areAnnotatedWith(layout.frameworkAnnotations().controller())
                .should()
                .haveSimpleNameEndingWith("Controller")
                .allowEmptyShould(true));
  }

  public static DcaRule restControllersEndWithRestControllerSuffix(DcaLayout layout) {
    return DcaRule.of(
        "DCA-NAM-006",
        "REST Controllers must end with '"
            + layout.restControllerSuffix()
            + "' (REST best practice)",
        "@RestController annotated classes should end with '"
            + layout.restControllerSuffix()
            + "' following RESTful naming conventions",
        arch ->
            classes()
                .that()
                .resideInAPackage(layout.incomingAdapterPattern())
                .and()
                .areAnnotatedWith(layout.frameworkAnnotations().restController())
                .should()
                .haveSimpleNameEndingWith(layout.restControllerSuffix())
                .allowEmptyShould(true));
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
                .resideInAPackage(layout.adapterPattern())
                .allowEmptyShould(true));
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
                .resideInAPackage(layout.adapterPattern())
                .allowEmptyShould(true));
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
                .allowEmptyShould(true));
  }

  public static DcaRule noTechnicalSuffixesInDomain(DcaLayout layout) {
    // Domain concepts carry ubiquitous-language names. 'Manager'/'Helper'/'Util' signal
    // a missing domain concept; 'Impl' signals naming by pattern instead of by specialty.
    return DcaRule.of(
        "DCA-NAM-010",
        "Domain classes must not use technical suffixes (Manager, Helper, Util, Impl)",
        "Domain names come from the ubiquitous language - name services by their specialty, not by"
            + " technical role",
        arch ->
            noClasses()
                .that()
                .resideInAPackage(layout.domainPattern())
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
                .allowEmptyShould(true));
  }

  public static DcaRule viewModelsResideInIncomingWebAdapters(DcaLayout layout) {
    String incomingWebPattern =
        layout.basePackage()
            + ".*."
            + layout.adapterSubpackage()
            + "."
            + layout.incomingSubpackage()
            + ".web..";
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
                .resideInAPackage(incomingWebPattern)
                .allowEmptyShould(true));
  }
}
