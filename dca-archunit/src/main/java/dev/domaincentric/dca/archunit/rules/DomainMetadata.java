package dev.domaincentric.dca.archunit.rules;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanBeAnnotated;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.FrameworkAnnotations;
import java.util.ArrayList;
import java.util.List;

/** Shared ownership and role-by-target policy for domain metadata. */
final class DomainMetadata {
  private DomainMetadata() {}

  static String owner(JavaClass type) {
    String tactical = "dev.domaincentric.dca.buildingblocks.ddd.tactical.";
    if (type.isAssignableTo(tactical + "DomainEvent")) return "DCA-ADV-004";
    if (type.isAssignableTo(tactical + "DomainService")) return "DCA-ADV-011";
    if (type.isAssignableTo(tactical + "Factory")) return "DCA-ADV-015";
    if (type.getSimpleName().endsWith("Specification")) return "DCA-ADV-018";
    return "DCA-ONI-003";
  }

  static void check(DcaArchitecture arch, String id) {
    FrameworkAnnotations roles = arch.layout().frameworkAnnotations();
    List<String> violations = new ArrayList<>();
    for (JavaClass type : arch.classes()) {
      if (type.isInterface()
          || !owner(type).equals(id)
          || !JavaClass.Predicates.resideInAnyPackage(arch.allDomainPatterns()).test(type))
        continue;
      if (id.equals("DCA-ONI-003")
          && !JavaClass.Predicates.resideInAnyPackage(arch.allDomainModelPatterns()).test(type))
        continue;
      inspect(
          type,
          type.getName(),
          violations,
          roles.injectable(),
          roles.persistenceEntity(),
          roles.transactional());
      type.getFields()
          .forEach(
              field ->
                  inspect(
                      field,
                      field.getFullName(),
                      violations,
                      roles.injectionSite(),
                      roles.persistenceMapping()));
      type.getMethods()
          .forEach(
              method ->
                  inspect(
                      method,
                      method.getFullName(),
                      violations,
                      roles.transactional(),
                      roles.eventListener(),
                      id.equals("DCA-ADV-004") ? List.of() : roles.injectionSite()));
      type.getConstructors()
          .forEach(ctor -> inspect(ctor, ctor.getFullName(), violations, roles.injectionSite()));
    }
    if (!violations.isEmpty())
      throw new AssertionError(
          id + ": prohibited domain metadata\n" + String.join("\n", violations));
  }

  @SafeVarargs
  private static void inspect(
      CanBeAnnotated target, String name, List<String> violations, List<String>... roles) {
    for (List<String> role : roles)
      for (String annotation : role) {
        if (target.isAnnotatedWith(annotation) || target.isMetaAnnotatedWith(annotation)) {
          violations.add(name + " carries prohibited metadata " + annotation);
        }
      }
  }
}
