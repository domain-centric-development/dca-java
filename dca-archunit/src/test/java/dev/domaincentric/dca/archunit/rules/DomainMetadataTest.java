package dev.domaincentric.dca.archunit.rules;

import static org.junit.jupiter.api.Assertions.*;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import dev.domaincentric.dca.archunit.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class DomainMetadataTest {
  private static final String ROOT = Fixtures.ROOT + ".metadata";

  private DcaArchitecture arch(String suffix) {
    var a = ROOT + ".annotations.";
    var roles =
        FrameworkAnnotations.none()
            .withInjectable(a + "Injectable")
            .withPersistenceEntity(a + "Entity")
            .withTransactional(a + "Transaction")
            .withInjectionSite(a + "Injection")
            .withPersistenceMapping(a + "Mapping")
            .withEventListener(a + "Listener");
    return DcaArchitecture.of(
        DcaLayout.forBasePackage(ROOT + suffix).withFrameworkAnnotations(roles),
        new ClassFileImporter().importPackages(ROOT + suffix));
  }

  @Test
  void coversEveryProhibitedCellAndExclusiveOwnership() {
    var arch = arch(".bad");
    var ids = List.of("DCA-ONI-003", "DCA-ADV-004", "DCA-ADV-011", "DCA-ADV-015", "DCA-ADV-018");
    var groups = List.of("Model", "Event", "Service", "Factory", "Spec");
    for (int i = 0; i < ids.size(); i++) {
      final String id = ids.get(i), group = groups.get(i);
      var rule =
          DcaRules.all(arch.layout()).stream()
              .filter(r -> r.id().equals(id))
              .findFirst()
              .orElseThrow();
      String message = assertThrows(AssertionError.class, () -> rule.check(arch)).getMessage();
      for (String cell :
          List.of(
              "TypeInjectable",
              "TypeEntity",
              "TypeTransaction",
              "FieldInjection",
              "FieldMapping",
              "MethodTransaction",
              "MethodListener",
              "ConstructorInjection",
              "TypeComposed")) assertTrue(message.contains(group + cell), message);
      if (!group.equals("Event")) assertTrue(message.contains(group + "MethodInjection"), message);
      for (String other : groups)
        if (!other.equals(group)) assertFalse(message.contains("model." + other), message);
    }
  }

  @Test
  void unclassifiedMetadataPassesOnEveryTarget() {
    var arch = arch(".good");
    for (String id :
        List.of("DCA-ONI-003", "DCA-ADV-004", "DCA-ADV-011", "DCA-ADV-015", "DCA-ADV-018"))
      DcaRules.all(arch.layout()).stream()
          .filter(r -> r.id().equals(id))
          .findFirst()
          .orElseThrow()
          .check(arch);
  }

  @Test
  void presetsAndCopiesPreserveMemberRoles() {
    for (var preset :
        List.of(
            FrameworkAnnotations.spring(),
            FrameworkAnnotations.jakarta(),
            FrameworkAnnotations.quarkus(),
            FrameworkAnnotations.micronaut())) {
      assertTrue(preset.injectionSite().contains("jakarta.inject.Inject"));
      assertTrue(preset.persistenceMapping().contains("jakarta.persistence.Column"));
      assertEquals(
          preset.injectionSite(),
          preset.named("custom").withInjectable("example.Component").injectionSite());
      assertEquals(
          preset.persistenceMapping(),
          preset.withPersistenceEntity("example.Entity").persistenceMapping());
    }
    assertTrue(FrameworkAnnotations.none().injectionSite().isEmpty());
    assertTrue(FrameworkAnnotations.none().persistenceMapping().isEmpty());
  }

  @Test
  void diagnosticReportsMissingStereotypeWithoutFailure() {
    var root = Fixtures.ROOT + ".conventions.containers";
    var layout =
        DcaLayout.forBasePackage(root).withFrameworkAnnotations(FrameworkAnnotations.spring());
    var architecture = DcaArchitecture.of(layout, new ClassFileImporter().importPackages(root));
    var bytes = new java.io.ByteArrayOutputStream();
    var original = System.out;
    try {
      System.setOut(new java.io.PrintStream(bytes));
      NamingRules.useCasesAreServices(layout).check(architecture);
    } finally {
      System.setOut(original);
    }
    assertTrue(
        bytes.toString().contains("application.usecases.placeorder.Place"), bytes.toString());
    assertTrue(
        bytes.toString().contains("register by configuration or annotate"), bytes.toString());
  }
}
