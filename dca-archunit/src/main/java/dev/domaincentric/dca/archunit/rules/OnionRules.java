package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.FrameworkAnnotations;
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
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .allowEmptyShould(true))
        .selecting("Classes in <module>.domain.. of every module root.")
        .checking(
            "No dependency on a class in an application package of any module root"
                + " (<module>.application..), the module's own included. Dependencies on adapters"
                + " or infrastructure are covered by other rules, not this one.");
  }

  public DcaRule domainMustBeFrameworkIndependent() {
    return DcaRule.of(
            "DCA-ONI-002",
            "The Domain Model should be framework independent and should not use 3rd party libraries"
                + " when possible",
            "Domain should be framework-independent (Dependency Inversion Principle)",
            arch -> {
              // Every discovered context's domain plus the shared kernel's own domain package — the
              // inclusion is explicit here, where the former base.*.domain.. wildcard covered the
              // shared kernel only as a side effect of matching one segment.
              List<String> domainPackageList = new ArrayList<>(List.of(arch.allDomainPatterns()));
              domainPackageList.add(DcaLayout.BUILDING_BLOCKS_TACTICAL_PACKAGE);
              domainPackageList.add(DcaLayout.BUILDING_BLOCKS_PORT_OUT_PACKAGE);
              String[] domainPackages = domainPackageList.toArray(String[]::new);
              List<String> allowed = new ArrayList<>(layout.thirdPartyPackagesAllowedInDomain());
              allowed.addAll(List.of(domainPackages));
              return classes()
                  .that()
                  .resideInAnyPackage(domainPackages)
                  .should()
                  .onlyDependOnClassesThat()
                  .resideInAnyPackage(allowed.toArray(String[]::new))
                  .allowEmptyShould(true);
            })
        .selecting(
            "Classes in <module>.domain.. of every module root, plus the classes of the"
                + " building-blocks packages ddd.tactical.. and hexagonal.port.out.. when they are"
                + " on the classpath under scan.")
        .checking(
            "Every dependency targets a class in one of those same packages or in an allowed"
                + " third-party package: by default java.., lombok.., org.apache.commons.lang3..,"
                + " org.apache.commons.collections4.. and org.jspecify.annotations.., plus"
                + " whatever the layout adds. The building-blocks strategic and port.in packages"
                + " are not on the list, nor is the shared kernel unless it is a module root with a"
                + " domain layer of its own. A dependency on any other package is reported.");
  }

  public DcaRule domainModelsMustNotHaveFrameworkAnnotations() {
    FrameworkAnnotations annotations = layout.frameworkAnnotations();
    return DcaRule.of(
            "DCA-ONI-003",
            "Domain Models must not carry container or persistence annotations",
            "Domain models are framework-independent: no injectable stereotype makes them a managed"
                + " component, no mapping annotation ties them to a persistence framework - the"
                + " outgoing adapter maps them",
            arch ->
                noClasses()
                    .that()
                    .resideInAnyPackage(arch.allDomainModelPatterns())
                    .should(
                        AnnotationRoles.beAnnotatedWithAny(
                            annotations.injectable(), annotations.persistenceEntity()))
                    .allowEmptyShould(true))
        .selecting("Classes in <module>.domain.model.. of every module root.")
        .checking(
            "None carries one of the configured injectable stereotypes or persistence-entity"
                + " annotations directly on the class. Only the configured annotations are checked;"
                + " other framework annotations, meta-annotations, and classes elsewhere in the"
                + " domain layer (domain.service, domain.event) are not. With both roles empty the"
                + " rule has nothing to forbid and passes.");
  }
}
