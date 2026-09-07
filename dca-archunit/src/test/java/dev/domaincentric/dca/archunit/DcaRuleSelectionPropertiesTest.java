package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DcaRuleSelectionPropertiesTest {

  private static final DcaLayout LAYOUT = DcaLayout.forBasePackage("com.acme.shop");

  private static Properties properties(String... lines) {
    Properties properties = new Properties();
    for (String line : lines) {
      int separator = line.indexOf('=');
      properties.setProperty(
          line.substring(0, separator).trim(), line.substring(separator + 1).trim());
    }
    return properties;
  }

  @Test
  void setsNarrowTheRun() {
    DcaRuleSelection selection =
        DcaRuleSelection.fromProperties(properties("dca.rules.sets = tactical, cycles"));

    assertEquals(
        List.of("tactical", "cycles"),
        DcaRules.select(LAYOUT, selection).stream().map(DcaRuleSet::name).toList());
  }

  @Test
  void offAndWarnCarryTheirReasons() {
    DcaRuleSelection selection =
        DcaRuleSelection.fromProperties(
            properties(
                "dca.rules.off = DCA-NAM-002,DCA-NAM-006",
                "dca.rules.warn = DCA-TAC-009",
                "dca.rule.DCA-NAM-002.reason = no DI framework in this project",
                "dca.rule.DCA-TAC-009.reason = made final step by step"));

    assertEquals(DcaSeverity.OFF, selection.severityOf("DCA-NAM-002"));
    assertEquals(DcaSeverity.OFF, selection.severityOf("DCA-NAM-006"));
    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-TAC-009"));
    assertEquals(
        "no DI framework in this project", selection.reasonFor("DCA-NAM-002").orElseThrow());
    assertEquals("made final step by step", selection.reasonFor("DCA-TAC-009").orElseThrow());
  }

  @Test
  void severityCanBeSetPerSet() {
    DcaRuleSelection selection =
        DcaRuleSelection.fromProperties(
            properties(
                "dca.rules.warn.sets = naming",
                "dca.rule.set.naming.reason = naming is being aligned"));

    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-NAM-001"));
    assertEquals("naming is being aligned", selection.reasonFor("DCA-NAM-001").orElseThrow());
  }

  @Test
  void aRuleLevelSettingWinsOverItsSet() {
    DcaRuleSelection selection =
        DcaRuleSelection.fromProperties(
            properties("dca.rules.warn.sets = naming", "dca.rules.off = DCA-NAM-002"));

    assertEquals(DcaSeverity.OFF, selection.severityOf("DCA-NAM-002"));
    assertEquals(DcaSeverity.WARN, selection.severityOf("DCA-NAM-001"));
  }

  @Test
  void freezeAndIgnoreAreRead() {
    DcaRuleSelection selection =
        DcaRuleSelection.fromProperties(
            properties(
                "dca.rules.freeze = DCA-ONI-002",
                "dca.rules.freeze.store = arch/frozen",
                "dca.rule.DCA-STR-003.ignore = .*backoffice.*"));

    assertTrue(selection.isFrozen("DCA-ONI-002"));
    assertEquals(Path.of("arch/frozen"), selection.freezeStore().orElseThrow());
    assertEquals(List.of(".*backoffice.*"), selection.ignoredViolationPatterns("DCA-STR-003"));
  }

  /**
   * The value of an {@code .ignore} key is one regular expression, commas included — {@code
   * Foo.{1,3}Bar} is a quantifier, not two patterns. Several expressions use indexed keys.
   */
  @Test
  void anIgnoreExpressionIsOneRegexCommasIncluded() {
    DcaRuleSelection selection =
        DcaRuleSelection.fromProperties(properties("dca.rule.DCA-STR-003.ignore = Foo.{1,3}Bar"));

    assertEquals(List.of("Foo.{1,3}Bar"), selection.ignoredViolationPatterns("DCA-STR-003"));
  }

  @Test
  void severalIgnoreExpressionsUseIndexedKeys() {
    DcaRuleSelection selection =
        DcaRuleSelection.fromProperties(
            properties(
                "dca.rule.DCA-STR-003.ignore.2 = .*generated.*",
                "dca.rule.DCA-STR-003.ignore.1 = .*legacy.*",
                "dca.rule.DCA-STR-003.ignore = .*[a-z],[0-9].*"));

    assertEquals(
        List.of(".*[a-z],[0-9].*", ".*legacy.*", ".*generated.*"),
        selection.ignoredViolationPatterns("DCA-STR-003"));
  }

  @Test
  void aTypoInARuleIdFailsLoudly() {
    IllegalArgumentException failure =
        assertThrows(
            IllegalArgumentException.class,
            () -> DcaRuleSelection.fromProperties(properties("dca.rules.off = DCA-NAM-042")));

    assertTrue(failure.getMessage().contains("DCA-NAM-042"));
  }

  @Test
  void anAbsentClasspathResourceMeansTheWholeCatalog() {
    DcaRuleSelection selection = DcaRuleSelection.fromClasspath("no-such-file.properties");

    assertEquals(DcaRules.all(LAYOUT).size(), DcaRules.selectFlat(LAYOUT, selection).size());
    assertEquals(DcaSeverity.ERROR, selection.severityOf("DCA-NAM-001"));
  }

  @Test
  void aFileIsReadTheSameWayAsTheClasspathResource(@TempDir Path directory) throws IOException {
    Path file = directory.resolve(DcaRuleSelection.DEFAULT_RESOURCE);
    Files.writeString(
        file, "dca.rules.off = DCA-NAM-002\ndca.rule.DCA-NAM-002.reason = no Spring\n");

    DcaRuleSelection selection = DcaRuleSelection.fromFile(file);

    assertEquals(DcaSeverity.OFF, selection.severityOf("DCA-NAM-002"));
    assertEquals("no Spring", selection.reasonFor("DCA-NAM-002").orElseThrow());
  }
}
