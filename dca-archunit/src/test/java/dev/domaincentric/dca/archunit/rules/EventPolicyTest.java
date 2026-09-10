package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.*;
import org.junit.jupiter.api.Test;

class EventPolicyTest {
  private static final String ROOT = Fixtures.ROOT + ".eventpolicy";

  @Test
  void eventFreeSaveIsExemptButRecordedAndUnresolvedRemainRequired() {
    String message = Fixtures.failure(ROOT, "DCA-USE-009").getMessage();
    assertFalse(message.contains("application.free"), message);
    // Reported by name: the aggregate that registers, the unresolvable type argument, and the
    // aggregate a nested class it never calls registers on (outside the hierarchy; another
    // top-level
    // class cannot reach the protected method).
    for (String required :
        new String[] {
          "application.recording.SaveUseCase",
          "application.unresolved.SaveUseCase",
          "application.external.SaveUseCase"
        }) assertTrue(message.contains(required), required + " missing in\n" + message);
    // Violation messages name the simple class: use isolated imports to distinguish equal suffixes.
    var classes =
        new ClassFileImporter()
            .importPackages(
                ROOT + ".module.domain",
                ROOT + ".module.application.shared",
                ROOT + ".module.application.free");
    var layout = DcaLayout.forBasePackage(ROOT);
    UseCaseRules.useCasesPublishDomainEventsAfterSaving(layout)
        .check(DcaArchitecture.of(layout, classes));
    var partial =
        new ClassFileImporter()
            .importPackages(ROOT + ".module.application.shared", ROOT + ".module.application.free");
    assertThrows(
        AssertionError.class,
        () ->
            UseCaseRules.useCasesPublishDomainEventsAfterSaving(layout)
                .check(DcaArchitecture.of(layout, partial)));
  }

  @Test
  void businessVersionIsAllowedButAdapterPlacementIsNot() {
    var arch = Fixtures.arch(ROOT);
    Fixtures.rule(ROOT, "DCA-ADV-006").check(arch);
    Fixtures.rule(ROOT, "DCA-ADV-007").check(arch);
    assertTrue(Fixtures.failure(ROOT, "DCA-STR-007").getMessage().contains("Misplaced"));
  }

  @Test
  void publicationAfterEmptyBoundaryIsAKnownStaticPass() {
    var arch = Fixtures.arch(ROOT);
    Fixtures.rule(ROOT, "DCA-USE-012").check(arch);
  }
}
