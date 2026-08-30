package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class DcaRuleSelectionTest {

  private static final DcaLayout LAYOUT = DcaLayout.forBasePackage("com.acme.shop");

  @Test
  void allSelectsTheWholeCatalog() {
    assertEquals(
        DcaRules.all(LAYOUT).size(), DcaRules.selectFlat(LAYOUT, DcaRuleSelection.all()).size());
  }

  @Test
  void onlySetsNarrowsTheRun() {
    List<DcaRuleSet> sets =
        DcaRules.select(LAYOUT, DcaRuleSelection.all().onlySets("tactical", "cycles"));

    assertEquals(List.of("tactical", "cycles"), sets.stream().map(DcaRuleSet::name).toList());
  }

  @Test
  void onlyIdsNarrowsTheRun() {
    List<DcaRule> rules =
        DcaRules.selectFlat(LAYOUT, DcaRuleSelection.all().onlyIds("DCA-NAM-001", "DCA-CYC-001"));

    assertEquals(
        List.of("DCA-CYC-001", "DCA-NAM-001"), rules.stream().map(DcaRule::id).sorted().toList());
  }

  @Test
  void excludedRuleKeepsItsReason() {
    DcaRuleSelection selection = DcaRuleSelection.all().excluding("DCA-NAM-002", "no DI framework");

    assertEquals(DcaSeverity.OFF, selection.severityOf("DCA-NAM-002"));
    assertEquals("no DI framework", selection.reasonFor("DCA-NAM-002").orElseThrow());
    assertEquals(DcaSeverity.ERROR, selection.severityOf("DCA-NAM-001"));
  }

  @Test
  void anExcludedRuleStillTakesPartInTheRun() {
    DcaRuleSelection selection = DcaRuleSelection.all().excluding("DCA-NAM-002");

    assertTrue(selection.includes("naming", "DCA-NAM-002"), "reported as skipped, not dropped");
  }

  @Test
  void severityCanBeSetForAWholeSet() {
    DcaRuleSelection selection = DcaRuleSelection.all().warningForSet("naming", "migrating");

    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-NAM-001"));
    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-NAM-011"));
    assertEquals(DcaSeverity.ERROR, selection.severityOf("DCA-TAC-001"));
  }

  @Test
  void ignorePatternsAccumulateAndSurviveASeverityChange() {
    DcaRuleSelection selection =
        DcaRuleSelection.all()
            .ignoringViolationsMatching("DCA-STR-003", ".*backoffice.*")
            .ignoringViolationsMatching("DCA-STR-003", ".*legacy.*")
            .warning("DCA-STR-003", "phased in");

    assertEquals(
        List.of(".*backoffice.*", ".*legacy.*"), selection.ignoredViolationPatterns("DCA-STR-003"));
    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-STR-003"));
  }

  @Test
  void mergingLetsTheLaterSelectionWinPerRule() {
    DcaRuleSelection base =
        DcaRuleSelection.all().warning("DCA-NAM-001", "base").excluding("DCA-NAM-003");
    DcaRuleSelection override = DcaRuleSelection.all().excluding("DCA-NAM-001", "override");

    DcaRuleSelection merged = base.mergedWith(override);

    assertEquals(DcaSeverity.OFF, merged.severityOf("DCA-NAM-001"));
    assertEquals("override", merged.reasonFor("DCA-NAM-001").orElseThrow());
    assertEquals(DcaSeverity.OFF, merged.severityOf("DCA-NAM-003"), "untouched entries survive");
  }

  @Test
  void frozenRulesAndStoreAreRecorded() {
    DcaRuleSelection selection =
        DcaRuleSelection.all().frozen("DCA-ONI-002").withFreezeStore(Path.of("arch/frozen"));

    assertTrue(selection.isFrozen("DCA-ONI-002"));
    assertFalse(selection.isFrozen("DCA-ONI-001"));
    assertEquals(Path.of("arch/frozen"), selection.freezeStore().orElseThrow());
  }

  @Test
  void unknownRuleIdIsRejected() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class, () -> DcaRuleSelection.all().excluding("DCA-NAM-999"));

    assertTrue(failure.getMessage().contains("DCA-NAM-999"));
  }

  @Test
  void unknownRuleSetIsRejected() {
    assertThrows(
        IllegalArgumentException.class, () -> DcaRuleSelection.all().onlySets("tacticall"));
  }

  @Test
  void invalidIgnorePatternIsRejected() {
    assertThrows(
        IllegalArgumentException.class,
        () -> DcaRuleSelection.all().ignoringViolationsMatching("DCA-NAM-001", "[unclosed"));
  }

  @Test
  void catalogMetadataIsComplete() {
    assertEquals(10, DcaRules.setNames().size());
    assertEquals(DcaRules.all(LAYOUT).size(), DcaRules.allIds().size());
  }
}
