package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import java.util.ArrayList;
import java.util.List;

/**
 * Layered Architecture rules: dependencies point inward, the domain knows no infrastructure, the
 * application layer talks to outbound ports only, and transactions are an application concern.
 */
public final class LayeredRules implements DcaRuleSet {

  private final DcaLayout layout;
  private final List<DcaRule> rules;

  public LayeredRules(DcaLayout layout) {
    this.layout = layout;
    this.rules =
        List.of(
            layeredArchitectureDiagnostic(),
            domainMustNotDependOnInfrastructure(),
            applicationMustNotUseInfrastructureImplementations(),
            transactionBoundariesBelongToApplicationLayer(),
            outputPortMarkersMustBeInterfaces());
  }

  @Override
  public String name() {
    return "layered";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  /**
   * Documentation-only: ArchUnit's {@code layeredArchitecture()} does not fit Ports and Adapters,
   * where both adapter types depend on the application layer. The hexagonal dependency rules are
   * enforced by {@link HexagonalRules} instead. This rule never fails.
   */
  public DcaRule layeredArchitectureDiagnostic() {
    return DcaRule.informational(
            "DCA-LAY-001",
            "Diagnostic: The rules of the Layered Architecture should be followed",
            "Traditional layering (application accessed only by incoming adapters) contradicts Ports"
                + " and Adapters, where outgoing adapters implement application-level output ports;"
                + " the hexagonal rules cover the intended dependency direction",
            arch -> {})
        .selecting(
            "Informational - selects nothing. ArchUnit's layered-architecture definition (adapter"
                + " layer accesses application, application accesses domain, domain accesses"
                + " nothing) is not built, because in Ports and Adapters outgoing adapters"
                + " implement application-level output ports.")
        .checking(
            "Informational - selects nothing and never fails; it carries doctrine only. The"
                + " dependency direction is enforced by the hexagonal rules.");
  }

  public DcaRule domainMustNotDependOnInfrastructure() {
    return DcaRule.of(
            "DCA-LAY-002",
            "Domain must not have dependencies on Infrastructure",
            "Domain should not depend on infrastructure concerns (Dependency Inversion Principle)",
            arch ->
                noClasses()
                    .that()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .should()
                    .dependOnClassesThat()
                    // The global infrastructure package and every module's own one.
                    .resideInAnyPackage(arch.allInfrastructurePatterns())
                    // A context may legitimately have no domain layer at all - a supporting or
                    // generic
                    // subdomain in transaction-script style. An absent domain is not a violation.
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.domain.. of every module root, the shared kernel's domain"
                + " included.")
        .checking(
            "No dependency on a class in the global infrastructure package"
                + " (<base>.infrastructure..) or in any module's own infrastructure package"
                + " (<module>.infrastructure..). The"
                + " shared kernel's infrastructure package is not in that list. A module without a"
                + " domain layer selects nothing and passes.");
  }

  public DcaRule applicationMustNotUseInfrastructureImplementations() {
    return DcaRule.of(
            "DCA-LAY-003",
            "Application Services must only use outbound ports (not infrastructure implementations)",
            "Application services should only use outbound ports declared as interfaces (port.out), not"
                + " infrastructure implementation details",
            arch ->
                noClasses()
                    .that()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .should()
                    .dependOnClassesThat(arch.infrastructureImplementation())
                    .allowEmptyShould(true))
        .selecting("Classes in <module>.application.. of every module root.")
        .checking(
            "No dependency on a class residing in the global infrastructure package or in any"
                + " isolated module's own infrastructure package, sub-packages included, with an"
                + " exact segment boundary. Dependencies on outgoing adapters are not checked"
                + " here - only infrastructure packages count.");
  }

  public DcaRule transactionBoundariesBelongToApplicationLayer() {
    String rationale =
        "Transactions are an application-layer concern - domain and incoming adapters must not"
            + " manage them";
    return DcaRule.check(
            "DCA-LAY-004",
            "Transaction boundaries belong to the application layer",
            rationale,
            arch -> {
              List<String> transactional = layout.frameworkAnnotations().transactional();
              List<String> transactionApi = layout.frameworkAnnotations().transactionApi();
              List<String> transactionManager = layout.frameworkAnnotations().transactionManager();
              List<String> allowedPatterns =
                  new ArrayList<>(List.of(arch.allApplicationPatterns()));
              allowedPatterns.add(
                  ".." + layout.adapterSubpackage() + "." + layout.outgoingSubpackage() + "..");
              String[] allowed = allowedPatterns.toArray(String[]::new);
              CollectedViolations violations = CollectedViolations.withoutHeader();
              violations.addAll(
                  methods()
                      .that(AnnotationRoles.annotatedWithAny(transactional))
                      .should()
                      .beDeclaredInClassesThat()
                      .resideInAnyPackage(allowed)
                      .allowEmptyShould(true),
                  arch.classes(),
                  rationale);
              violations.addAll(
                  classes()
                      .that(AnnotationRoles.annotatedWithAny(transactional))
                      .should()
                      .resideInAnyPackage(allowed)
                      .allowEmptyShould(true),
                  arch.classes(),
                  rationale);
              // Programmatic boundaries, two kinds. Using a transaction API (template, user
              // transaction) draws a boundary and is allowed exactly where the annotation is.
              // Depending on a transaction manager or on DCA's TransactionBoundary port is wiring
              // and plumbing as well: the composition root (the global infrastructure package) and
              // the shared kernel's infrastructure may do that too. The boundary's implementations
              // are exempt wherever they live.
              DescribedPredicate<JavaClass> usesTransactionApi =
                  DescribedPredicate.describe(
                      "a configured transaction API", c -> transactionApi.contains(c.getName()));
              violations.addAll(
                  noClasses()
                      .that()
                      .resideOutsideOfPackages(allowed)
                      .and()
                      .areNotAssignableTo(TransactionBoundary.class)
                      .should()
                      .dependOnClassesThat(usesTransactionApi)
                      .allowEmptyShould(true),
                  arch.classes(),
                  rationale);
              List<String> wiringAllowed = new ArrayList<>(allowedPatterns);
              wiringAllowed.add(layout.infrastructurePattern());
              wiringAllowed.add(
                  layout.sharedKernelPackage() + "." + layout.infrastructureSubpackage() + "..");
              DescribedPredicate<JavaClass> managerOrBoundary =
                  DescribedPredicate.describe(
                      "a configured transaction manager or TransactionBoundary",
                      c ->
                          transactionManager.contains(c.getName())
                              || c.isAssignableTo(TransactionBoundary.class));
              violations.addAll(
                  noClasses()
                      .that()
                      .resideOutsideOfPackages(wiringAllowed.toArray(String[]::new))
                      .and()
                      .areNotAssignableTo(TransactionBoundary.class)
                      .should()
                      .dependOnClassesThat(managerOrBoundary)
                      .allowEmptyShould(true),
                  arch.classes(),
                  rationale);
              violations.throwIfAny();
            })
        .selecting(
            "Three selections. Declarative: methods and classes under scan that carry one of the"
                + " configured transactional annotations directly (meta-annotations do not count)."
                + " Transaction use: classes under scan that depend on one of the configured"
                + " transaction-API types (role transactionApi - a transaction template or user"
                + " transaction, the types code runs a transaction with). Wiring: classes under scan"
                + " that depend on one of the configured transaction-manager types (role"
                + " transactionManager) or on TransactionBoundary. A dependency counts at any depth"
                + " (field, parameter, call); implementations of TransactionBoundary itself are never"
                + " selected. With the roles empty only TransactionBoundary dependencies are"
                + " selected.")
        .checking(
            "Annotations and transaction use: the method is declared in, or the class resides in,"
                + " an application package of some module root (<module>.application..) or an"
                + " outgoing adapter package (..adapter.outgoing..) anywhere - a domain,"
                + " incoming-adapter or infrastructure package is reported, the global"
                + " infrastructure package included. Wiring: additionally allowed in the global"
                + " infrastructure package (<base>.infrastructure.., the composition root that"
                + " declares the manager) and in the shared kernel's infrastructure package"
                + " (<base>.sharedkernel.infrastructure.., plumbing that hooks into the boundary);"
                + " a domain, incoming-adapter or module-infrastructure package is reported. All"
                + " findings are collected into one violation. Where manager and boundary"
                + " dependencies are allowed the rule cannot tell wiring from a call: a class in the"
                + " global or shared-kernel infrastructure package that obtains the manager and"
                + " begins a transaction itself passes. Which transaction a boundary opens is not"
                + " checked.");
  }

  public DcaRule outputPortMarkersMustBeInterfaces() {
    return DcaRule.of(
            "DCA-LAY-005",
            "No implementation is placed into the building-blocks output-port package",
            "port.out contains outbound port interfaces (Repository, OutputPort, DomainEventPublisher)"
                + " shared across all bounded contexts. These must be interfaces to ensure the"
                + " application layer remains framework-independent and follows the Dependency"
                + " Inversion Principle. Implementations belong in infrastructure or adapter packages.",
            arch ->
                classes()
                    .that()
                    .resideInAPackage(DcaLayout.BUILDING_BLOCKS_PORT_OUT_PACKAGE)
                    .should()
                    .beInterfaces()
                    .allowEmptyShould(true))
        .selecting(
            "Classes in the building-blocks package hexagonal.port.out.. on the classpath under"
                + " scan.")
        .checking(
            "Each is an interface. The project's own output ports in application.shared are not"
                + " selected; when the building-blocks package is not imported, the rule passes.");
  }
}
