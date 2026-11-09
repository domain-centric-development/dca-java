package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.Fixtures;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

/** Self-test of {@link OnionRules} against the shared hexagonal fixture tree. */
class OnionRulesTest {

  private static final String FIXTURES = Fixtures.ROOT + ".hexagonal";
  private static final String ASYNCHRONY = Fixtures.ROOT + ".asynchrony";

  @TestFactory
  Stream<DynamicTest> goodFixturePasses() {
    return Fixtures.goodFixturePasses(OnionRules::new, FIXTURES + ".good");
  }

  /** Every onion rule has a negative fixture. */
  @TestFactory
  Stream<DynamicTest> badFixtureFails() {
    return Fixtures.badFixtureFails(
        OnionRules::new,
        FIXTURES + ".bad",
        // negative fixture in fixtures.asynchrony, see below
        "DCA-ONI-004");
  }

  @Test
  @DisplayName("DCA-ONI-004 passes a synchronous domain whose use case awaits")
  void aSynchronousDomainPasses() {
    Fixtures.passes(ASYNCHRONY + ".good", "DCA-ONI-004");
  }

  @Test
  @DisplayName(
      "DCA-ONI-004 reports a future and a nested publisher type in the domain, not the application"
          + " layer and not an awaitable of the code base's own")
  void asynchronousTypesInTheDomainAreReported() {
    String message = Fixtures.failure(ASYNCHRONY + ".bad", "DCA-ONI-004").getMessage();
    assertTrue(message.contains("domain.Ledger"), message);
    assertTrue(message.contains("domain.PostingFeed"), "nested type: " + message);
    assertFalse(message.contains("RecordPostingUseCase"), "application layer: " + message);
    assertFalse(message.contains("domain.Posting."), "own awaitable: " + message);
  }

  @Test
  @DisplayName("DCA-ONI-004 follows the configured asynchronous types")
  void theAsynchronousTypesAreConfigurable() {
    String pkg = ASYNCHRONY + ".bad";
    DcaLayout own =
        DcaLayout.forBasePackage(pkg).withAsynchronousTypes(pkg + ".ledger.domain.Deferred");
    String message = assertThrows(AssertionError.class, () -> check(own, pkg)).getMessage();
    assertTrue(message.contains("domain.Posting."), message);
    assertFalse(message.contains("domain.Ledger."), "the default list is replaced: " + message);

    check(DcaLayout.forBasePackage(pkg).withAsynchronousTypes(), pkg);
  }

  private static void check(DcaLayout layout, String pkg) {
    DcaRule rule =
        new OnionRules(layout)
            .rules().stream().filter(r -> r.id().equals("DCA-ONI-004")).findFirst().orElseThrow();
    rule.check(DcaArchitecture.of(layout, new ClassFileImporter().importPackages(pkg)));
  }
}
