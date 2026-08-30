package dev.domaincentric.dca.archunit;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import com.tngtech.archunit.library.freeze.TextFileBasedViolationStore;
import com.tngtech.archunit.library.freeze.ViolationStore;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Runs a single {@link DcaRule} the way a {@link DcaRuleSelection} asks for it: at its configured
 * severity, against a frozen baseline where one was requested, with the tolerated violations
 * filtered out.
 *
 * <p>Nothing here throws on a violation — the caller decides what a {@link DcaRuleOutcome} means.
 * {@link dev.domaincentric.dca.archunit.junit.DcaArchitectureTest} turns it into a failed, aborted
 * or passing test; {@link DcaRules#checkAll(DcaArchitecture)} into an {@link AssertionError}.
 */
public final class DcaRuleExecution {

  private DcaRuleExecution() {}

  /** Evaluates one rule under the given selection. */
  public static DcaRuleOutcome execute(
      DcaRule rule, DcaArchitecture architecture, DcaRuleSelection selection) {
    DcaSeverity severity = selection.severityOf(rule.id());
    if (severity == DcaSeverity.OFF) {
      return new DcaRuleOutcome(
          rule.id(),
          DcaRuleOutcome.Status.SKIPPED,
          selection.reasonFor(rule.id()).orElse("switched off, no reason recorded"));
    }

    List<Pattern> ignored = compile(selection.ignoredViolationPatterns(rule.id()));
    Optional<String> violations = evaluate(rule, architecture, selection, ignored);
    if (violations.isEmpty()) {
      return new DcaRuleOutcome(rule.id(), DcaRuleOutcome.Status.PASSED, null);
    }
    String message = withReason(violations.get(), selection.reasonFor(rule.id()));
    return severity == DcaSeverity.WARN
        ? new DcaRuleOutcome(rule.id(), DcaRuleOutcome.Status.WARNED, message)
        : new DcaRuleOutcome(rule.id(), DcaRuleOutcome.Status.FAILED, message);
  }

  /** The violation report, or empty when the rule holds. */
  private static Optional<String> evaluate(
      DcaRule rule,
      DcaArchitecture architecture,
      DcaRuleSelection selection,
      List<Pattern> ignored) {
    Optional<ArchRule> archRule = rule.archRule(architecture);
    boolean frozen = selection.isFrozen(rule.id());

    if (archRule.isEmpty()) {
      if (frozen) {
        throw new IllegalArgumentException(
            "Rule "
                + rule.id()
                + " cannot be frozen: it runs several checks internally and has no single ArchUnit"
                + " rule to build a baseline from. Lower it to WARN instead.");
      }
      return runAndCatch(rule, architecture, ignored);
    }

    ArchRule effective =
        frozen
            ? FreezingArchRule.freeze(archRule.get()).persistIn(store(selection))
            : archRule.get();
    EvaluationResult result = effective.evaluate(architecture.classes());
    if (!ignored.isEmpty()) {
      result = result.filterDescriptionsMatching(description -> !matchesAny(description, ignored));
    }
    return result.hasViolation()
        ? Optional.of(result.getFailureReport().toString())
        : Optional.empty();
  }

  /** Fallback for rules that run their checks themselves. */
  private static Optional<String> runAndCatch(
      DcaRule rule, DcaArchitecture architecture, List<Pattern> ignored) {
    try {
      rule.check(architecture);
      return Optional.empty();
    } catch (DcaRuleViolation violation) {
      if (ignored.isEmpty()) {
        return Optional.of(violation.getMessage());
      }
      List<String> remaining =
          violation.violations().stream().filter(v -> !matchesAny(v, ignored)).toList();
      return remaining.isEmpty()
          ? Optional.empty()
          : Optional.of(violation.retaining(remaining).getMessage());
    } catch (AssertionError violation) {
      // A rule that delegates to several ArchUnit rules: filter the report's lines instead.
      String message =
          violation.getMessage() == null ? violation.toString() : violation.getMessage();
      if (ignored.isEmpty()) {
        return Optional.of(message);
      }
      List<String> lines = new ArrayList<>(List.of(message.split("\n", -1)));
      String header = lines.isEmpty() ? "" : lines.remove(0);
      lines.removeIf(line -> matchesAny(line, ignored));
      if (lines.stream().allMatch(String::isBlank)) {
        return Optional.empty();
      }
      lines.add(0, header);
      return Optional.of(String.join("\n", lines));
    }
  }

  /**
   * A text-file store configured from the selection rather than from ArchUnit's global {@code
   * archunit.properties}: {@code FreezingArchRule} initialises its store itself, so the settings
   * have to be substituted at that moment.
   */
  private static ViolationStore store(DcaRuleSelection selection) {
    TextFileBasedViolationStore delegate = new TextFileBasedViolationStore();
    Properties configured = new Properties();
    selection
        .freezeStore()
        .map(Path::toString)
        .ifPresent(path -> configured.setProperty("default.path", path));
    configured.setProperty("default.allowStoreCreation", "true");
    configured.setProperty("default.allowStoreUpdate", "true");
    return new ViolationStore() {
      @Override
      public void initialize(Properties ignoredGlobalProperties) {
        delegate.initialize(configured);
      }

      @Override
      public boolean contains(ArchRule rule) {
        return delegate.contains(rule);
      }

      @Override
      public void save(ArchRule rule, List<String> violations) {
        delegate.save(rule, violations);
      }

      @Override
      public List<String> getViolations(ArchRule rule) {
        return delegate.getViolations(rule);
      }
    };
  }

  private static String withReason(String violations, Optional<String> reason) {
    return reason.map(text -> violations + "\n\nRecorded reason: " + text).orElse(violations);
  }

  private static boolean matchesAny(String text, List<Pattern> patterns) {
    return patterns.stream().anyMatch(pattern -> pattern.matcher(text).find());
  }

  private static List<Pattern> compile(List<String> regexes) {
    List<Pattern> patterns = new ArrayList<>(regexes.size());
    for (String regex : regexes) {
      patterns.add(Pattern.compile(regex, Pattern.DOTALL));
    }
    return patterns;
  }
}
