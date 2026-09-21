package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaMarkers;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.FrameworkAnnotations;
import dev.domaincentric.dca.buildingblocks.application.UseCaseException;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Error-handling rules: a failure carries the name of what went wrong, in the layer that knows it.
 *
 * <p>Three layers, three kinds of failure. A broken rule of the model is a {@link DomainException}
 * in the domain layer; a request the application cannot serve is a {@link UseCaseException} in the
 * application layer; the translation into a protocol answer happens in the incoming adapter, which
 * is the only layer that knows the protocol. Argument guards are outside all of this — a null or
 * range check states a contract for the caller and keeps the platform's own argument exception.
 *
 * <p>The rules pin the two base types and the layers they live in, and keep transport vocabulary
 * out of the inner layers. Whether a specific {@code throw} should have been a domain exception is
 * not decidable from the import model: a {@code throw} site is not part of it. That judgement stays
 * with review.
 */
public final class ErrorHandlingRules implements DcaRuleSet {

  private final List<DcaRule> rules;

  public ErrorHandlingRules(DcaLayout layout) {
    this.rules =
        List.of(
            domainExceptionsExtendDomainException(),
            exceptionsResideInTheirLayer(),
            applicationExceptionsExtendUseCaseException(),
            exceptionsCarryNoFrameworkMetadata(layout),
            exceptionsNameNoTransportConcept(),
            adaptersWithoutTranslation());
  }

  @Override
  public String name() {
    return "errors";
  }

  @Override
  public List<DcaRule> rules() {
    return rules;
  }

  public DcaRule domainExceptionsExtendDomainException() {
    return DcaRule.of(
            "DCA-ERR-001",
            "Exceptions declared in the domain layer must extend DomainException",
            "A failure the model raises is a broken business rule and says so through its type; a"
                + " caller that cannot tell one from the platform's own failures cannot answer"
                + " either of them properly",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allDomainPatterns())
                    .and()
                    .areAssignableTo(Throwable.class)
                    .should()
                    .beAssignableTo(arch.layout().markers().domainException())
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.domain.. of every module root that are assignable to Throwable -"
                + " the exception types the domain layer declares itself. The base type"
                + " DomainException lives in the building blocks and is not selected.")
        .checking(
            "Each extends DomainException, directly or through an intermediate base class. Where"
                + " the exception is thrown is not checked - a throw site is not part of the import"
                + " model - so a guard that raises the platform's argument exception is outside"
                + " this rule. An empty selection passes.");
  }

  public DcaRule exceptionsResideInTheirLayer() {
    return DcaRule.check(
            "DCA-ERR-002",
            "Domain and use-case exceptions reside in the layer whose failure they name",
            "The base type states which layer owns the failure; declaring it elsewhere puts the"
                + " vocabulary of that layer outside it and lets an adapter invent failures the"
                + " application never reports",
            arch -> {
              CollectedViolations collected =
                  CollectedViolations.withHeader(
                      "An exception resides outside the layer its base type names");
              collected.addAll(
                  classes()
                      .that()
                      .areAssignableTo(arch.layout().markers().domainException())
                      .and()
                      .resideOutsideOfPackages(
                          arch.layout().markers().declaringPackagePatterns().toArray(String[]::new))
                      .should()
                      .resideInAnyPackage(arch.allDomainPatterns())
                      .allowEmptyShould(true),
                  arch.classes(),
                  "a domain exception belongs to the domain layer");
              collected.addAll(
                  classes()
                      .that()
                      .areAssignableTo(arch.layout().markers().useCaseException())
                      .and()
                      .resideOutsideOfPackages(
                          arch.layout().markers().declaringPackagePatterns().toArray(String[]::new))
                      .should()
                      .resideInAnyPackage(arch.allApplicationPatterns())
                      .allowEmptyShould(true),
                  arch.classes(),
                  "a use-case exception belongs to the application layer");
              collected.throwIfAny();
            })
        .selecting(
            "Classes anywhere on the classpath under scan that are assignable to the domain-exception"
                + " or the use-case-exception role, except the vocabulary's own code: the packages the"
                + " configured role types live in are excluded, so a project's own base class is not"
                + " reported as residing outside a layer it never claimed.")
        .checking(
            "A subtype of DomainException resides in <module>.domain.. of some module root, a"
                + " subtype of UseCaseException in <module>.application... Both findings are"
                + " collected into one violation. Which of the two a given failure should have been"
                + " is not checked here. An empty selection passes.");
  }

  public DcaRule applicationExceptionsExtendUseCaseException() {
    return DcaRule.of(
            "DCA-ERR-003",
            "Exceptions declared in the application layer must extend UseCaseException",
            "A use case that cannot serve a request reports why through its type, so that one"
                + " incoming adapter can map each outcome onto its protocol and another adapter can"
                + " map the same outcome onto a different one",
            arch ->
                classes()
                    .that()
                    .resideInAnyPackage(arch.allApplicationPatterns())
                    .and()
                    .areAssignableTo(Throwable.class)
                    .should()
                    .beAssignableTo(arch.layout().markers().useCaseException())
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.application.. of every module root that are assignable to Throwable"
                + " - the exception types the application layer declares itself. The base type"
                + " UseCaseException lives in the building blocks and is not selected.")
        .checking(
            "Each extends UseCaseException, directly or through an intermediate base class. A"
                + " subtype of DomainException declared in an application package is reported here"
                + " as well: a failure of the model belongs to the model. An empty selection"
                + " passes.");
  }

  public DcaRule exceptionsCarryNoFrameworkMetadata(DcaLayout layout) {
    return DcaRule.check(
            "DCA-ERR-004",
            "Domain and use-case exceptions must not carry prohibited framework metadata",
            "An exception of an inner layer that carries container, persistence or protocol"
                + " metadata has decided how the outside answers it, which is the incoming"
                + " adapter's decision and only its; an annotation that fixes the answer's status"
                + " decides it for every protocol at once, including the ones the exception knows"
                + " nothing about",
            arch -> {
              FrameworkAnnotations roles = layout.frameworkAnnotations();
              CollectedViolations collected =
                  CollectedViolations.withHeader("Prohibited metadata on an inner-layer exception");
              for (JavaClass type : arch.classes()) {
                if (!isProjectException(type, arch.layout().markers())) {
                  continue;
                }
                for (List<String> role :
                    List.of(
                        roles.injectable(),
                        roles.persistenceEntity(),
                        roles.transactional(),
                        roles.webController(),
                        roles.restController(),
                        roles.eventListener(),
                        roles.transportStatus())) {
                  for (String annotation : role) {
                    collected.require(
                        !type.isAnnotatedWith(annotation) && !type.isMetaAnnotatedWith(annotation),
                        type.getName() + " carries prohibited metadata " + annotation);
                  }
                }
              }
              collected.throwIfAny();
            })
        .selecting(
            "Classes anywhere on the classpath under scan that are assignable to DomainException or"
                + " to UseCaseException, the building-blocks package excluded.")
        .checking(
            "The type carries none of the configured injectable, persistence-entity, transactional,"
                + " web-controller, REST-controller, event-listener or transport-status"
                + " annotations, directly or as a meta-annotation. The transport-status role is the"
                + " one that fixes the protocol answer on the exception itself; where the framework"
                + " answers through a mapper class instead of an annotation the role is empty and"
                + " nothing is selected for it. Fields and methods are not inspected, and an"
                + " annotation the layout classifies into no role is allowed. With those roles"
                + " empty the rule selects no metadata and passes.");
  }

  public DcaRule exceptionsNameNoTransportConcept() {
    return DcaRule.check(
            "DCA-ERR-005",
            "Domain and use-case exception names must stay in the language of their layer",
            "A failure named after a transport concept is a decision about the answer, taken in a"
                + " layer that does not know the protocol; the name should say what went wrong, not"
                + " what the caller should be told",
            arch -> {
              CollectedViolations collected =
                  CollectedViolations.withHeader(
                      "A technical or transport word in an exception name");
              for (JavaClass type : arch.classes()) {
                if (!isProjectException(type, arch.layout().markers())) {
                  continue;
                }
                String name = type.getSimpleName();
                for (String suffix : FORBIDDEN_SUFFIXES) {
                  collected.require(
                      !name.endsWith(suffix),
                      type.getName() + " ends with the technical suffix " + suffix);
                }
                for (String word : FORBIDDEN_WORDS) {
                  collected.require(
                      !name.contains(word),
                      type.getName() + " names the transport concept " + word);
                }
              }
              collected.throwIfAny();
            })
        .selecting(
            "Classes anywhere on the classpath under scan that are assignable to DomainException or"
                + " to UseCaseException, the building-blocks package excluded.")
        .checking(
            "The simple name ends with none of Error, Fault, Failure and contains none of Http,"
                + " Status, Response. Whether the remaining name is a term of the Ubiquitous"
                + " Language is not decidable here and stays with review. An empty selection"
                + " passes.");
  }

  public DcaRule adaptersWithoutTranslation() {
    return DcaRule.informational(
            "DCA-ERR-006",
            "Diagnostic: incoming adapter packages that drive a use case without translating its"
                + " failures",
            "A package that drives the application and knows neither failure type either lets"
                + " everything escape to a generic handler or catches a generic type and answers"
                + " every outcome the same way; whether it does is not visible in the import model",
            arch -> {
              Map<String, List<JavaClass>> drivingByPackage = new TreeMap<>();
              Set<String> packagesThatName = new HashSet<>();
              for (JavaClass type : arch.classes()) {
                if (!JavaClass.Predicates.resideInAnyPackage(arch.allIncomingAdapterPatterns())
                    .test(type)) {
                  continue;
                }
                if (dependsOnAssignableTo(type, arch.layout().markers().domainException())
                    || dependsOnAssignableTo(type, arch.layout().markers().useCaseException())) {
                  packagesThatName.add(type.getPackageName());
                }
                if (!type.isInterface()
                    && dependsOnAssignableTo(type, arch.layout().markers().inputPort())) {
                  drivingByPackage
                      .computeIfAbsent(type.getPackageName(), pkg -> new ArrayList<>())
                      .add(type);
                }
              }
              drivingByPackage.forEach(
                  (pkg, driving) -> {
                    if (packagesThatName.contains(pkg)) {
                      return;
                    }
                    System.out.println(
                        "[DCA-ERR-006] "
                            + pkg
                            + ": drives an input port and names no failure type of the inner layers"
                            + " ("
                            + driving.stream().map(JavaClass::getSimpleName).sorted().toList()
                            + ")");
                  });
            })
        .selecting(
            "Incoming adapter packages of every module root (<module>.adapter.incoming..) that hold"
                + " at least one non-interface class depending on a class assignable to InputPort -"
                + " the packages that drive the application.")
        .checking(
            "Informational diagnostic only: lists a package when no class in it - a central"
                + " exception handler beside the adapters included - depends on a class assignable"
                + " to DomainException or UseCaseException, and never fails. A handler in a"
                + " different package of the same module is not seen, and a caught type is not"
                + " visible to the import model, so this does not establish that a package"
                + " translates nothing.");
  }

  private static final List<String> FORBIDDEN_SUFFIXES = List.of("Error", "Fault", "Failure");

  private static final List<String> FORBIDDEN_WORDS = List.of("Http", "Status", "Response");

  /** A project's own exception type: assignable to a base type, outside the building blocks. */
  private static boolean isProjectException(JavaClass type, DcaMarkers markers) {
    return (type.isAssignableTo(markers.domainException())
            || type.isAssignableTo(markers.useCaseException()))
        && !markers.declaresTypesIn(type.getPackageName());
  }

  private static boolean dependsOnAssignableTo(JavaClass type, String target) {
    return type.getDirectDependenciesFromSelf().stream()
        .anyMatch(dependency -> dependency.getTargetClass().isAssignableTo(target));
  }
}
