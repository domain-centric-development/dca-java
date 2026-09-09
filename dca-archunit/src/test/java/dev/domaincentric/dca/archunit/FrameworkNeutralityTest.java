package dev.domaincentric.dca.archunit;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The rules and the building blocks are the foundation any production system builds on — any
 * industry, any framework. Their texts flow verbatim into the knowledge catalog, so they must not
 * speak Spring, and they must not speak shop. This test keeps that mechanical: a framework or shop
 * word in a rule's title, rationale, {@code selects} or {@code checks}, or in a building block's
 * javadoc prose, fails the build — unless the sentence explicitly marks it as an example ("for
 * example", "e.g.", "such as").
 */
class FrameworkNeutralityTest {

  /**
   * Framework vocabulary: product names and the Spring/JPA annotation names by their short form.
   */
  private static final Pattern FRAMEWORK =
      Pattern.compile(
          "\\b(Spring|Modulith|JPA|Jakarta|jakarta|Hibernate|Quarkus|Micronaut)\\b"
              + "|@(Service|Component|Transactional\\w*|ApplicationModule\\w*|Controller|RestController"
              + "|EventListener|Entity|Table|ApplicationScoped|Singleton)\\b");

  /**
   * Shop vocabulary. {@code Order} only capitalised — the lower-case word is ordinary English ("in
   * order to", "catalog order").
   */
  private static final Pattern SHOP =
      Pattern.compile(
          "(?i)\\b(carts?|checkout|products?|inventory|pricing|customers?|shop)\\b|\\bOrders?\\b");

  private static final Pattern EXAMPLE_MARKER =
      Pattern.compile("for\\s+example|e\\.g\\.|such\\s+as|one\\s+implementation|for\\s+instance");

  private static final Pattern CODE =
      Pattern.compile("<pre>.*?</pre>|\\{@(?:code|link|linkplain)\\s[^}]*}", Pattern.DOTALL);

  /** DDD's relationship pattern, not the shop's customer. */
  private static final Pattern DDD_TERMS = Pattern.compile("Customer[–/-]Supplier");

  @Test
  @DisplayName("no rule text names a framework or the shop except as an example")
  void ruleTextsAreNeutral() {
    List<String> offences = new ArrayList<>();
    for (DcaRule rule : DcaRules.all(DcaLayout.forBasePackage("com.example"))) {
      for (String text : List.of(rule.title(), rule.rationale(), rule.selects(), rule.checks())) {
        offences.addAll(offencesIn(rule.id(), text));
      }
    }
    assertTrue(offences.isEmpty(), "\n" + String.join("\n", offences));
  }

  @Test
  @DisplayName("no building-block javadoc names a framework or the shop except as an example")
  void buildingBlockJavadocIsNeutral() throws IOException {
    Path sources = Path.of("..", "dca-building-blocks", "src", "main", "java");
    assertTrue(Files.isDirectory(sources), "expected the sibling module at " + sources);
    List<String> offences = new ArrayList<>();
    try (Stream<Path> files = Files.walk(sources)) {
      for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
        String javadoc = javadocProse(Files.readString(file));
        offences.addAll(offencesIn(file.getFileName().toString(), javadoc));
      }
    }
    assertTrue(offences.isEmpty(), "\n" + String.join("\n", offences));
  }

  /** Every offending sentence, prefixed with where it was found. */
  private static List<String> offencesIn(String where, String text) {
    List<String> offences = new ArrayList<>();
    String prose = DDD_TERMS.matcher(CODE.matcher(text).replaceAll(" ")).replaceAll(" ");
    for (String sentence : prose.split("(?<=[.;])\\s+")) {
      if (EXAMPLE_MARKER.matcher(sentence).find()) {
        continue;
      }
      Matcher framework = FRAMEWORK.matcher(sentence);
      Matcher shop = SHOP.matcher(sentence);
      if (framework.find()) {
        offences.add(where + ": framework word '" + framework.group() + "' in: " + sentence.trim());
      } else if (shop.find()) {
        offences.add(where + ": shop word '" + shop.group() + "' in: " + sentence.trim());
      }
    }
    return offences;
  }

  /** The javadoc comments of a source file, comment markers and leading asterisks removed. */
  private static String javadocProse(String source) {
    StringBuilder prose = new StringBuilder();
    Matcher comments = Pattern.compile("/\\*\\*(.*?)\\*/", Pattern.DOTALL).matcher(source);
    while (comments.find()) {
      String body = comments.group(1).replaceAll("(?m)^\\s*\\* ?", "");
      prose.append(body).append("\n\n");
    }
    return prose.toString();
  }
}
