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
            + " rules)");
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
    for (DcaRuleSet set : sets) {
      sb.append("## `").append(set.name()).append("`\n\n");
      sb.append(
          "| Id | Rule | Rationale | Selects | Checks |\n|----|------|-----------|---------|--------|\n");
      for (DcaRule rule : set.rules()) {
        sb.append("| `")
            .append(rule.id())
            .append("` | ")
            .append(escapeCell(rule.title()))
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
    return sb.toString();
  }

  /**
   * The catalog as JSON: {@code [{"set":…,"id":…,"title":…,"rationale":…,"selects":…,"checks":…},
   * …]}.
   */
  public static String json() {
    StringBuilder sb = new StringBuilder("[\n");
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
            .append('}');
      }
    }
    return sb.append("\n]\n").toString();
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
