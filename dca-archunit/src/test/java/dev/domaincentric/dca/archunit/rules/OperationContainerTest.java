package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.*;
import org.junit.jupiter.api.Test;

class OperationContainerTest {
  @Test
  void containerCycleUsesMarkerDiscovery() {
    String pkg = Fixtures.ROOT + ".conventions.cycles";
    var layout = DcaLayout.forBasePackage(pkg).withOperationContainers("usecases");
    var arch = DcaArchitecture.of(layout, new ClassFileImporter().importPackages(pkg));
    assertThrows(
        AssertionError.class, () -> CycleRules.applicationSlicesFreeOfCycles(layout).check(arch));
  }

  @Test
  void constructionChecksCallerRoleAndContextNotVisibility() {
    String message =
        Fixtures.violation(Fixtures.ROOT + ".construction", "DCA-TAC-005").getMessage();
    for (String caller : new String[] {"CreationUseCase", "Persistence", "ForeignFactory"})
      org.junit.jupiter.api.Assertions.assertTrue(message.contains(caller), message);
    org.junit.jupiter.api.Assertions.assertFalse(
        message.contains("LineFactory constructs"), message);
    org.junit.jupiter.api.Assertions.assertFalse(
        message.contains("OtherAggregate constructs"), message);
  }

  @Test
  void localPortsOutgoingResponsesAndDomainManagersAreAllowed() {
    String pkg = Fixtures.ROOT + ".conventions.containers";
    for (String id :
        new String[] {"DCA-NAM-010", "DCA-USE-008", "DCA-TAC-014", "DCA-TAC-019", "DCA-TAC-021"}) {
      Fixtures.rule(pkg, id).check(Fixtures.arch(pkg));
    }
  }

  @Test
  void containersNormalizeOperationRootsAndKeepMixedLayoutsInvalid() {
    for (String kind : new String[] {"containers", "mixed"}) {
      String pkg = Fixtures.ROOT + ".conventions." + kind;
      DcaLayout plain = DcaLayout.forBasePackage(pkg);
      DcaLayout configured = plain.withOperationContainers("usecases");
      var classes = new ClassFileImporter().importPackages(pkg);
      assertThrows(
          AssertionError.class,
          () ->
              UseCaseRules.useCasePackagesUseOneDepth(plain)
                  .check(DcaArchitecture.of(plain, classes)));
      if (kind.equals("containers"))
        UseCaseRules.useCasePackagesUseOneDepth(configured)
            .check(DcaArchitecture.of(configured, classes));
      else
        assertThrows(
            AssertionError.class,
            () ->
                UseCaseRules.useCasePackagesUseOneDepth(configured)
                    .check(DcaArchitecture.of(configured, classes)));
    }
  }
}
