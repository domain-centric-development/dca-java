package dev.domaincentric.dca.archunit.junit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRuleSelection;
import dev.domaincentric.dca.archunit.DcaSeverity;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * The base class composes three configuration sources. A project that configures rules in code must
 * not silently lose a {@code dca-archunit.properties} added later — which is why the hook to
 * override is {@link DcaArchitectureTest#additionalSelection()}, not {@code selection()}.
 */
class DcaArchitectureTestSelectionTest {

  /** Stands in for the properties file, which is absent on this test's class path. */
  private static final DcaRuleSelection FILE =
      DcaRuleSelection.all().warning("DCA-TAC-009", "from the file");

  private static class Configured extends DcaArchitectureTest {
    @Override
    protected DcaLayout layout() {
      return DcaLayout.forBasePackage("com.acme.shop");
    }

    @Override
    protected DcaRuleSelection selection() {
      return FILE.mergedWith(super.selection());
    }

    @Override
    protected Set<String> excludedRuleIds() {
      return Set.of("DCA-NAM-001");
    }

    @Override
    protected DcaRuleSelection additionalSelection() {
      return DcaRuleSelection.all().excluding("DCA-STR-003", "from the code");
    }
  }

  @Test
  void fileLegacyOverridesAndCodeAllContribute() {
    DcaRuleSelection selection = new Configured().selection();

    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-TAC-009"), "from the properties file");
    assertEquals(DcaSeverity.OFF, selection.severityOf("DCA-NAM-001"), "from excludedRuleIds()");
    assertEquals(
        DcaSeverity.OFF, selection.severityOf("DCA-STR-003"), "from additionalSelection()");
    assertEquals("from the code", selection.reasonFor("DCA-STR-003").orElseThrow());
  }

  /**
   * A properties file written for an earlier release names a retired id; the notice must survive
   * the merge with the selection made in code, on whichever side the retired id stands.
   */
  @Test
  void aRetiredIdTheFileNamesIsStillReportedAfterTheMerge() {
    DcaRuleSelection file = DcaRuleSelection.all().excluding("DCA-HEX-015", "several instances");
    DcaRuleSelection code = DcaRuleSelection.all().warning("DCA-TAC-009", "from the code");

    assertEquals(Set.of("DCA-HEX-015"), file.mergedWith(code).referencedRetiredIds());
    assertEquals(Set.of("DCA-HEX-015"), code.mergedWith(file).referencedRetiredIds());
    assertEquals(
        Set.of("DCA-HEX-015"),
        file.mergedWith(DcaRuleSelection.all())
            .mergedWith(DcaRuleSelection.all())
            .referencedRetiredIds(),
        "the chain DcaArchitectureTest builds: file, legacy overrides, additional selection");
  }

  @Test
  void anEmptyAdditionalSelectionChangesNothing() {
    DcaRuleSelection selection = FILE.mergedWith(DcaRuleSelection.all());

    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-TAC-009"));
    assertEquals("from the file", selection.reasonFor("DCA-TAC-009").orElseThrow());
  }
}
