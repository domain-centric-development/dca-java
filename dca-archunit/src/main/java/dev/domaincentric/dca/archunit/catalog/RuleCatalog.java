package dev.domaincentric.dca.archunit.catalog;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.DcaRules;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Renders the rule catalog — every rule set with id, title, rationale and the two mechanics
 * descriptions ({@code selects}, {@code checks}) — as markdown ({@code RULES.md}) and as
 * machine-readable JSON ({@code rules.json}, consumed by the DCA knowledge catalog).
 *
 * <p>Run via {@code ./gradlew :dca-archunit:rulesCatalog} or {@code java -cp … RuleCatalog
 * <outputDir>}.
 */
public final class RuleCatalog {

  private static final DcaLayout PLACEHOLDER_LAYOUT = DcaLayout.forBasePackage("com.example");

  private RuleCatalog() {}

  public static void main(String[] args) throws IOException {
    Path out = Path.of(args.length > 0 ? args[0] : ".");
    Files.createDirectories(out);
    Files.writeString(out.resolve("RULES.md"), markdown(), StandardCharsets.UTF_8);
    Files.writeString(out.resolve("rules.json"), json(), StandardCharsets.UTF_8);
    System.out.println(
        "Wrote "
            + out.resolve("RULES.md")
            + " and "
            + out.resolve("rules.json")
            + " ("
            + DcaRules.all(PLACEHOLDER_LAYOUT).size()
            + " entries; "
            + counts()
            + ")");
  }

  private static String counts() {
    var all = DcaRules.all(PLACEHOLDER_LAYOUT);
    long info = all.stream().filter(r -> r.kind() == DcaRule.Kind.INFORMATIONAL).count();
    return (all.size() - info)
        + " enforced, "
        + info
        + " informational, "
        + DcaRules.retired().size()
        + " retired, 0 n/a";
  }

  /** The catalog as markdown. */
  public static String markdown() {
    List<DcaRuleSet> sets = DcaRules.ruleSets(PLACEHOLDER_LAYOUT);
    StringBuilder sb = new StringBuilder();
    sb.append("# DCA rule catalog\n\n");
    sb.append("Generated from `dca-archunit` — do not edit. ")
        .append(DcaRules.all(PLACEHOLDER_LAYOUT).size())
        .append(" rules in ")
        .append(sets.size())
        .append(" sets.\n\n");
    sb.append(counts()).append("\n\n");
    for (DcaRuleSet set : sets) {
      sb.append("## `").append(set.name()).append("`\n\n");
      sb.append(
          "| Id | Rule | Rationale | Selects | Checks |\n|----|------|-----------|---------|--------|\n");
      for (DcaRule rule : set.rules()) {
        sb.append("| `")
            .append(rule.id())
            .append("` | ")
            .append(escapeCell(rule.title()))
            .append(rule.kind() == DcaRule.Kind.INFORMATIONAL ? " (informational)" : "")
            .append(" | ")
            .append(escapeCell(rule.rationale()))
            .append(" | ")
            .append(escapeCell(rule.selects()))
            .append(" | ")
            .append(escapeCell(rule.checks()))
            .append(" |\n");
      }
      sb.append('\n');
    }
    sb.append("## Retired identities\n\n");
    DcaRules.retired().entrySet().stream()
        .sorted(java.util.Map.Entry.comparingByKey())
        .forEach(
            e ->
                sb.append("- `")
                    .append(e.getKey())
                    .append("` — ")
                    .append(e.getValue().reason())
                    .append("; replacement: ")
                    .append(e.getValue().replacement())
                    .append("; since ")
                    .append(e.getValue().since())
                    .append('\n'));
    return sb.toString();
  }

  /**
   * The version of {@code dca-archunit} this catalog describes, so that a consumer of {@code
   * rules.json} — the knowledge catalog, an agent, a rendered rule list — can tell which release a
   * rule text belongs to. The Gradle task passes the artifact's version; a run without it says so
   * rather than inventing a number.
   */
  private static String version() {
    String configured = System.getProperty("dca.catalog.version");
    if (configured != null && !configured.isBlank()) {
      return configured;
    }
    String packaged = RuleCatalog.class.getPackage().getImplementationVersion();
    return packaged == null || packaged.isBlank() ? "unspecified" : packaged;
  }

  /**
   * The catalog as JSON: {@code [{"set":…,"id":…,"title":…,"rationale":…,"selects":…,"checks":…},
   * …]}.
   */
  public static String json() {
    StringBuilder sb =
        new StringBuilder("{\n\"library\": ")
            .append(quote("dca-archunit"))
            .append(",\n\"version\": ")
            .append(quote(version()))
            .append(",\n\"rules\": [\n");
    boolean first = true;
    for (DcaRuleSet set : DcaRules.ruleSets(PLACEHOLDER_LAYOUT)) {
      for (DcaRule rule : set.rules()) {
        if (!first) {
          sb.append(",\n");
        }
        first = false;
        sb.append("  {\"set\": ")
            .append(quote(set.name()))
            .append(", \"id\": ")
            .append(quote(rule.id()))
            .append(", \"title\": ")
            .append(quote(rule.title()))
            .append(", \"rationale\": ")
            .append(quote(rule.rationale()))
            .append(", \"selects\": ")
            .append(quote(rule.selects()))
            .append(", \"checks\": ")
            .append(quote(rule.checks()))
            .append(", \"status\": ")
            .append(quote(rule.kind().name().toLowerCase(java.util.Locale.ROOT)))
            .append('}');
      }
    }
    sb.append("\n],\n\"retired\": [\n");
    boolean firstRetired = true;
    for (var entry : new java.util.TreeMap<>(DcaRules.retired()).entrySet()) {
      if (!firstRetired) sb.append(",\n");
      firstRetired = false;
      var value = entry.getValue();
      sb.append("{\"id\": ")
          .append(quote(entry.getKey()))
          .append(", \"reason\": ")
          .append(quote(value.reason()))
          .append(", \"replacement\": ")
          .append(quote(value.replacement()))
          .append(", \"since\": ")
          .append(quote(value.since()))
          .append('}');
    }
    return sb.append("\n]\n}\n").toString();
  }

  private static String escapeCell(String s) {
    return s.replace("|", "\\|").replace("\n", " ");
  }

  private static String quote(String s) {
    StringBuilder sb = new StringBuilder("\"");
    for (char c : s.toCharArray()) {
      switch (c) {
        case '"' -> sb.append("\\\"");
        case '\\' -> sb.append("\\\\");
        case '\n' -> sb.append("\\n");
        case '\r' -> sb.append("\\r");
        case '\t' -> sb.append("\\t");
        default -> {
          if (c < 0x20) {
            sb.append(String.format("\\u%04x", (int) c));
          } else {
            sb.append(c);
          }
        }
      }
    }
    return sb.append('"').toString();
  }
}
