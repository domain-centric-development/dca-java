package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Self-test of {@link HexagonalRules} against the shared hexagonal fixture tree. */
class HexagonalRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".hexagonal";
  private static final String INFRASTRUCTURE = Fixtures.ROOT + ".infrastructure";

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(HexagonalRules::new, FIXTURES + ".good");
  }

  /** Every hexagonal rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(HexagonalRules::new, FIXTURES + ".bad");
  }

  /**
   * The infrastructure predicate is exact: the root infrastructure package itself counts, and so
   * does a module's own {@code infrastructure} package — a class in either is an implementation
   * detail, wherever below the module it is imported from.
   */
  @Test
  @DisplayName("DCA-HEX-004 sees a class directly in the global infrastructure package")
  void incomingAdapterDependingOnRootInfrastructureIsReported() {
    String message = Fixtures.failure(INFRASTRUCTURE, "DCA-HEX-004").getMessage();
    assertTrue(message.contains("CartController") && message.contains("Wiring"), message);
  }

  @Test
  @DisplayName("DCA-HEX-005 sees a module's own infrastructure package")
  void outgoingAdapterDependingOnModuleInfrastructureIsReported() {
    String message = Fixtures.failure(INFRASTRUCTURE, "DCA-HEX-005").getMessage();
    assertTrue(message.contains("CartStorage") && message.contains("CartWiring"), message);
    assertFalse(
        message.contains("Lifecycle"),
        "the shared kernel's infrastructure is shared support, not a module's detail: " + message);
  }
}
