package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.*;

import dev.domaincentric.dca.archunit.*;
import org.junit.jupiter.api.Test;

class OperationPolicyTest {
  private static final String ROOT = Fixtures.ROOT + ".operations.";

  @Test
  void invocationFindsDirectPortAndTransitiveHelpers() {
    String message = Fixtures.failure(ROOT + "bad", "DCA-USE-016").getMessage();
    for (String name :
        new String[] {
          "DirectCallerUseCase",
          "PortCallerUseCase",
          "flat.HelperCallerUseCase",
          "feature.grouped.HelperCallerUseCase",
          "[via "
        }) assertTrue(message.contains(name), message);
    Fixtures.rule(ROOT + "good", "DCA-USE-016").check(Fixtures.arch(ROOT + "good"));
  }

  @Test
  void callerSideExclusionPermitsOnlyCoordinatorAndNeverRemovesCycles() {
    for (String kind : new String[] {"coord", "cycle"}) {
      var arch = Fixtures.arch(ROOT + kind);
      var rule = Fixtures.rule(ROOT + kind, "DCA-USE-016");
      assertThrows(AssertionError.class, () -> rule.check(arch));
      String caller =
          ROOT
              + kind
              + ".module.application."
              + (kind.equals("cycle") ? "feature." : "")
              + "coordinator.CoordinatorUseCase";
      var selection =
          DcaRuleSelection.all()
              .ignoringViolationsMatching(
                  rule.id(), "^" + java.util.regex.Pattern.quote(caller) + " -> ");
      var result = DcaRuleExecution.execute(rule, arch, selection);
      assertEquals(
          kind.equals("coord") ? DcaRuleOutcome.Status.PASSED : DcaRuleOutcome.Status.FAILED,
          result.status(),
          result.toString());
      if (kind.equals("cycle"))
        assertThrows(
            AssertionError.class, () -> Fixtures.rule(ROOT + kind, "DCA-CYC-005").check(arch));
    }
  }

  @Test
  void effectivePublicSurfaceIncludesInheritedAndUnrelatedMethods() {
    String message = Fixtures.failure(ROOT + "surfacebad", "DCA-USE-017").getMessage();
    for (String name :
        new String[] {"ExtraUseCase", "InheritedUseCase", "UnrelatedUseCase", "getX", "setX"})
      assertTrue(message.contains(name), message);
    Fixtures.rule(ROOT + "surfacegood", "DCA-USE-017").check(Fixtures.arch(ROOT + "surfacegood"));
  }

  @Test
  void aclEvidenceBelongsToEachUpstreamWithinOneAdapterPackage() {
    String root = Fixtures.ROOT + ".interaction.";
    Fixtures.rule(root + "good", "DCA-MAP-008").check(Fixtures.arch(root + "good"));
    String message = Fixtures.failure(root + "bad", "DCA-MAP-008").getMessage();
    assertTrue(message.contains("towards 'v'"), message);
    assertFalse(message.contains("towards 'u'"), message);
  }
}
