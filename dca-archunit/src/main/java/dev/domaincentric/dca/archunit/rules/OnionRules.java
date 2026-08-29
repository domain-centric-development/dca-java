package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Onion Architecture rules: the domain is the innermost layer — it depends on nothing but itself,
 * the building-blocks markers and a short allow-list of third-party packages.
 */
public final class OnionRules implements DcaRuleSet {

  private final DcaLayout layout;
  private final List<DcaRule> rules;

  public OnionRules(DcaLayout layout) {
    this.layout = layout;
    this.rules =
        List.of(
            domainMustNotAccessApplication(),
            domainMustBeFrameworkIndependent(),
            domainModelsMustNotHaveFrameworkAnnotations());
  }

  @Override
  public String name() {
    return "onion";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  public DcaRule domainMustNotAccessApplication() {
    return DcaRule.of(
        "DCA-ONI-001",
        "Domain must not access Application Services (Onion Architecture - Domain is innermost"
            + " layer)",
        "Domain is the innermost layer in onion architecture and should not depend on application"
            + " services",
        arch ->
            noClasses()
                .that()
                .resideInAnyPackage(layout.domainPattern())
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(layout.applicationPattern()));
  }

  public DcaRule domainMustBeFrameworkIndependent() {
    return DcaRule.of(
        "DCA-ONI-002",
        "The Domain Model should be framework independent and should not use 3rd party libraries"
            + " when possible",
        "Domain should be framework-independent (Dependency Inversion Principle)",
        arch -> {
          // Matched by pattern, never by context name: domainPattern() is base.*.domain.., which
          // also covers the shared kernel's own domain package.
          String[] domainPackages = {
            layout.domainPattern(),
            DcaLayout.BUILDING_BLOCKS_TACTICAL_PACKAGE,
            DcaLayout.BUILDING_BLOCKS_PORT_OUT_PACKAGE
          };
          List<String> allowed = new ArrayList<>(layout.thirdPartyPackagesAllowedInDomain());
          allowed.addAll(List.of(domainPackages));
          return classes()
              .that()
              .resideInAnyPackage(domainPackages)
              .should()
              .onlyDependOnClassesThat()
              .resideInAnyPackage(allowed.toArray(String[]::new));
        });
  }

  public DcaRule domainModelsMustNotHaveFrameworkAnnotations() {
    return DcaRule.of(
        "DCA-ONI-003",
        "Domain Models must not have Spring/JPA annotations",
        "Domain models must be framework-independent (no Spring or JPA annotations)",
        arch ->
            noClasses()
                .that()
                .resideInAnyPackage(layout.domainModelPattern(), layout.sharedKernelDomainPattern())
                .should()
                .beAnnotatedWith(layout.frameworkAnnotations().component())
                .orShould()
                .beAnnotatedWith(layout.frameworkAnnotations().service())
                .orShould()
                .beAnnotatedWith("jakarta.persistence.Entity")
                .orShould()
                .beAnnotatedWith("jakarta.persistence.Table"));
  }
}
