package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaMarkers;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
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
            domainModelsMustNotHaveFrameworkAnnotations(),
            domainLayerMustStaySynchronous());
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
                    .that(arch.inDomain())
                    .should()
                    .dependOnClassesThat(arch.inApplication())
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
              // The domain packages of every module root - the shared kernel among them when it
              // owns a domain package - plus the packages the marker vocabulary declares the
              // domain-facing roles in: the tactical markers and the outgoing ports. Derived from
              // the roles rather than hard-wired, so a project that points the roles at its own
              // markers is not reported as depending on a foreign library inside its own domain.
              // Strategic annotations, the application-layer roles and input ports are
              // deliberately not on the list.
              DescribedPredicate<JavaClass> domain =
                  arch.inDomain()
                      .or(
                          JavaClass.Predicates.resideInAnyPackage(
                              layout
                                  .markers()
                                  .declaringPackagePatternsOf(DcaMarkers.DOMAIN_FACING_ROLES)
                                  .toArray(String[]::new)));
              DescribedPredicate<JavaClass> allowed =
                  domain.or(
                      JavaClass.Predicates.resideInAnyPackage(
                          layout.thirdPartyPackagesAllowedInDomain().toArray(String[]::new)));
              return classes()
                  .that(domain)
                  .should()
                  .onlyDependOnClassesThat(allowed)
                  .allowEmptyShould(true);
            })
        .selecting(
            "Classes in <module>.domain.. of every module root, plus the classes of the packages"
                + " the configured marker vocabulary declares its domain-facing roles in — the"
                + " tactical roles and the outgoing ports — when they are on the classpath under"
                + " scan. With the default vocabulary those are the building-blocks packages"
                + " ddd.tactical.. and hexagonal.port.out..; with a project's own markers they are"
                + " the packages those markers live in.")
        .checking(
            "Every dependency targets a class in one of those same packages or in an allowed"
                + " third-party package: by default java.., lombok.., org.apache.commons.lang3..,"
                + " org.apache.commons.collections4.. and org.jspecify.annotations... A logging"
                + " facade is deliberately not among them - which logging, validation or utility"
                + " library a domain model may see is a project decision, made with"
                + " withThirdPartyPackagesAllowedInDomain(...). The vocabulary's own packages are on the list"
                + " because they are derived from the roles, so pointing a role at another"
                + " library's type does not make that library a reported dependency. The"
                + " application-layer roles and the incoming ports are not on the list, nor are"
                + " the strategic annotations, nor the shared kernel unless it is a module root"
                + " with a domain layer of its own. A dependency on any other package is"
                + " reported.");
  }

  public DcaRule domainModelsMustNotHaveFrameworkAnnotations() {
    return DcaRule.check(
            "DCA-ONI-003",
            "Domain models must not carry prohibited framework metadata",
            "Domain objects carry no metadata for container management, persistence or transaction coordination",
            arch -> DomainMetadata.check(arch, "DCA-ONI-003"))
        .selecting(
            "Non-interface domain models in domain packages. Metadata ownership is exclusive: events, services, factories, specifications, then domain.model types.")
        .checking(
            "Direct or meta-annotations: types prohibit injectable, persistenceEntity and transactional roles; fields prohibit injectionSite and persistenceMapping; methods prohibit transactional and eventListener, plus injectionSite except on events; constructors prohibit injectionSite. Unclassified annotations are allowed by this check. Empty configured roles select no metadata; wiring is not established.");
  }

  /**
   * DCA-ONI-004 (experimental). The domain layer names none of the configured asynchronous types
   * ({@link DcaLayout#withAsynchronousTypes(String...)}).
   */
  public DcaRule domainLayerMustStaySynchronous() {
    return DcaRule.of(
            "DCA-ONI-004",
            "Domain layer must stay synchronous",
            "Asynchrony is an I/O concern of ports and adapters; a synchronous domain model stays"
                + " testable, deterministic and free of sync-over-async hazards",
            arch -> {
              List<String> asynchronous = arch.layout().asynchronousTypes();
              DescribedPredicate<JavaClass> asynchronousType =
                  DescribedPredicate.describe(
                      "a configured asynchronous type",
                      c -> isOrIsNestedIn(c.getBaseComponentType().getName(), asynchronous));
              return noClasses()
                  .that(arch.inDomain())
                  .should()
                  .dependOnClassesThat(asynchronousType)
                  .allowEmptyShould(true);
            })
        .experimental()
        .selecting("Types the layout places in the domain layer of every module root.")
        .checking(
            "No dependency on one of the configured asynchronous types, on a type nested in one or"
                + " on a generic form of one - in a signature, a field, an annotation or a method"
                + " body. The default names the platform's standard asynchronous types; other"
                + " awaitables, asynchronous sequences and a library's own reactive types are not"
                + " checked unless the layout setting for asynchronous types names them. The"
                + " application and adapter layers are not selected. An empty list checks nothing;"
                + " an empty selection passes.");
  }

  /** The type itself, a generic form of it or a type nested in it. */
  private static boolean isOrIsNestedIn(String name, List<String> types) {
    for (String type : types) {
      if (name.equals(type) || name.startsWith(type + "$")) {
        return true;
      }
    }
    return false;
  }
}
