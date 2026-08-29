package dev.domaincentric.dca.archunit.junit;

import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRules;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * JUnit 5 base class that runs the whole DCA rule catalog as dynamic tests — one test per rule,
 * named {@code [DCA-TAC-001] Aggregate roots must …}.
 *
 * <pre>{@code
 * class ArchitectureTest extends DcaArchitectureTest {
 *   @Override
 *   protected DcaLayout layout() {
 *     return DcaLayout.forBasePackage("com.acme.shop");
 *   }
 * }
 * }</pre>
 *
 * <p>Override {@link #excludedRuleIds()} to switch individual rules off, or {@link #rules()} to
 * pick rule sets ({@code DcaRules.only(layout(), "tactical", "hexagonal")}).
 *
 * <p>Requires JUnit Jupiter on the test class path (it is a {@code compileOnly} dependency of this
 * library).
 */
public abstract class DcaArchitectureTest {

  private DcaArchitecture architecture;

  /** The layout of the project under test. */
  protected abstract DcaLayout layout();

  /** Identifiers of rules to skip. Default: none. */
  protected Set<String> excludedRuleIds() {
    return Set.of();
  }

  /** The rules to run. Default: the whole catalog minus {@link #excludedRuleIds()}. */
  protected List<DcaRule> rules() {
    return DcaRules.allExcept(layout(), excludedRuleIds());
  }

  /** The imported architecture; loaded once per test instance. */
  protected DcaArchitecture architecture() {
    if (architecture == null) {
      architecture = DcaArchitecture.load(layout());
    }
    return architecture;
  }

  @TestFactory
  Stream<DynamicTest> dcaRules() {
    DcaArchitecture arch = architecture();
    return rules().stream()
        .map(
            rule ->
                DynamicTest.dynamicTest(
                    "[" + rule.id() + "] " + rule.title(), () -> rule.check(arch)));
  }
}
