package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
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
    return DcaRule.check(
        "DCA-LAY-001",
        "Diagnostic: The rules of the Layered Architecture should be followed",
        "Traditional layering (application accessed only by incoming adapters) contradicts Ports"
            + " and Adapters, where outgoing adapters implement application-level output ports;"
            + " the hexagonal rules cover the intended dependency direction",
        arch -> {});
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
                .resideInAPackage(layout.infrastructurePattern())
                // A context may legitimately have no domain layer at all - a supporting or generic
                // subdomain in transaction-script style. An absent domain is not a violation.
                .allowEmptyShould(true));
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
                .dependOnClassesThat(arch.infrastructureImplementation()));
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
          String transactional = layout.frameworkAnnotations().transactional();
          List<String> allowedPatterns = new ArrayList<>(List.of(arch.allApplicationPatterns()));
          allowedPatterns.add(
              ".." + layout.adapterSubpackage() + "." + layout.outgoingSubpackage() + "..");
          String[] allowed = allowedPatterns.toArray(String[]::new);
          methods()
              .that()
              .areAnnotatedWith(transactional)
              .should()
              .beDeclaredInClassesThat()
              .resideInAnyPackage(allowed)
              .because(rationale)
              .allowEmptyShould(true)
              .check(arch.classes());
          classes()
              .that()
              .areAnnotatedWith(transactional)
              .should()
              .resideInAnyPackage(allowed)
              .because(rationale)
              .allowEmptyShould(true)
              .check(arch.classes());
        });
  }

  public DcaRule outputPortMarkersMustBeInterfaces() {
    return DcaRule.of(
        "DCA-LAY-005",
        "The shared kernel's output-port markers must all be interfaces",
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
                .allowEmptyShould(true));
  }
}
