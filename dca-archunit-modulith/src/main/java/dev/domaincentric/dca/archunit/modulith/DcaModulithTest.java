package dev.domaincentric.dca.archunit.modulith;

import static org.junit.jupiter.api.Assertions.assertFalse;

import dev.domaincentric.dca.archunit.DcaLayout;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModule;
import org.springframework.modulith.core.ApplicationModules;

/**
 * JUnit 5 base class for Spring Modulith's module verification, shaped like {@code
 * DcaArchitectureTest} so a project subclasses both the same way:
 *
 * <pre>{@code
 * class ModulithTest extends DcaModulithTest {
 *   @Override
 *   protected DcaLayout layout() {
 *     return DcaLayout.forBasePackage("com.acme.shop");
 *   }
 * }
 * }</pre>
 *
 * <p>What this checks is Modulith's own model — declared {@code allowedDependencies}, named
 * interfaces, open and closed modules — and what only Modulith knows. Cycles, layer violations and
 * leaking ports are the DCA catalog's business ({@code dca-archunit}); {@code DCA-MAP-006} checks,
 * without Spring, that the {@code @Upstream} declarations and Modulith's {@code
 * allowedDependencies} agree. Two base classes, two test classes — the artifacts stay independent
 * on purpose.
 *
 * <p>Requires {@code spring-modulith-core} and JUnit Jupiter on the test class path (both are
 * {@code compileOnly} here; a Modulith project has them through {@code
 * spring-modulith-starter-test}).
 */
public abstract class DcaModulithTest {

  private ApplicationModules modules;

  /** The layout of the project under test; only its base package matters here. */
  protected abstract DcaLayout layout();

  /** The modules, built once per test instance with the test-class filter applied. */
  protected ApplicationModules modules() {
    if (modules == null) {
      modules = ModulithModules.of(layout());
    }
    return modules;
  }

  @Test
  void moduleStructureIsValid() {
    modules().verify();
  }

  @Test
  void diagnosticListsTheDiscoveredModules() {
    List<ApplicationModule> list = modules().stream().toList();
    StringBuilder out = new StringBuilder("=== Spring Modulith application modules ===\n");
    for (ApplicationModule module : list) {
      out.append("  ")
          .append(module.getIdentifier())
          .append(": ")
          .append(module.getBasePackage())
          .append('\n');
      module
          .getNamedInterfaces()
          .forEach(ni -> out.append("    named interface ").append(ni.getName()).append('\n'));
    }
    System.out.print(out);
    assertFalse(list.isEmpty(), "no application modules found below " + layout().basePackage());
  }
}
