package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import java.util.List;

/**
 * Package cycle detection: no circular dependencies between the per-context slices of one layer
 * (domain model, application, incoming adapters, outgoing adapters).
 *
 * <p>Reference: Clean Architecture, Acyclic Dependencies Principle (ADP).
 */
public final class CycleRules implements DcaRuleSet {

  private final List<DcaRule> rules;

  public CycleRules(DcaLayout layout) {
    this.rules =
        List.of(
            domainPackagesFreeOfCycles(layout),
            applicationLayerFreeOfCycles(layout),
            outgoingAdaptersFreeOfCycles(layout),
            incomingAdaptersFreeOfCycles(layout));
  }

  @Override
  public String name() {
    return "cycles";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  public static DcaRule domainPackagesFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
        "DCA-CYC-001",
        "Domain Packages must not have cyclic dependencies",
        "Domain model packages should have clear boundaries and no cycles (Acyclic Dependencies"
            + " Principle)",
        arch ->
            slices()
                .matching(layout.basePackage() + ".(*)." + layout.domainSubpackage() + ".model..")
                .should()
                .beFreeOfCycles());
  }

  public static DcaRule applicationLayerFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
        "DCA-CYC-002",
        "Application Layer must not have cyclic dependencies",
        "Application services should have clear boundaries and no cycles",
        arch ->
            slices()
                .matching(layout.basePackage() + ".(*)." + layout.applicationSubpackage() + "..")
                .should()
                .beFreeOfCycles());
  }

  public static DcaRule outgoingAdaptersFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
        "DCA-CYC-003",
        "Outgoing Adapter Packages must not have cyclic dependencies",
        "Outgoing adapters should have clear boundaries and no cycles",
        arch ->
            slices()
                .matching(
                    layout.basePackage()
                        + ".(*)."
                        + layout.adapterSubpackage()
                        + "."
                        + layout.outgoingSubpackage()
                        + "..")
                .should()
                .beFreeOfCycles());
  }

  public static DcaRule incomingAdaptersFreeOfCycles(DcaLayout layout) {
    return DcaRule.of(
        "DCA-CYC-004",
        "Incoming Adapter Packages must not have cyclic dependencies",
        "Incoming adapters should have clear boundaries and no cycles",
        arch ->
            slices()
                .matching(
                    layout.basePackage()
                        + ".(*)."
                        + layout.adapterSubpackage()
                        + "."
                        + layout.incomingSubpackage()
                        + "..")
                .should()
                .beFreeOfCycles());
  }
}
