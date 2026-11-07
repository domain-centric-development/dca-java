package dev.domaincentric.dca.archunit.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnitAccess;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.PackageMatcher;
import dev.domaincentric.dca.archunit.DcaArchitecture;
import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.DcaMarkers;
import dev.domaincentric.dca.archunit.DcaRule;
import dev.domaincentric.dca.archunit.DcaRuleSet;
import dev.domaincentric.dca.archunit.FrameworkAnnotations;
import dev.domaincentric.dca.buildingblocks.application.OutputPortException;
import dev.domaincentric.dca.buildingblocks.application.PortException;
import dev.domaincentric.dca.buildingblocks.application.UseCaseException;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Error-handling rules: a failure carries the name of what went wrong, in the layer that knows it.
 *
 * <p>Three layers, three kinds of failure. A broken rule of the model is a {@link DomainException}
 * in the domain layer; a failure a port's contract declares is a {@link PortException} in the
 * application layer - a request the application cannot serve a {@link UseCaseException} (an input
 * port's failure), a conflict an output port reports an {@link OutputPortException}; the
 * translation into a protocol answer happens in the incoming adapter, which is the only layer that
 * knows the protocol. Argument guards are outside all of this — a null or range check states a
 * contract for the caller and keeps the platform's own argument exception.
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
            adaptersWithoutTranslation(),
            failuresCarryIdsAndFigures(),
            closedResultsRefuse(),
            domainExceptionsConstructedInTheDomain());
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
                    .that(arch.inDomain())
                    .and()
                    .areAssignableTo(Throwable.class)
                    .should(LayerConditions.be(arch.layout().markers().is("domainException")))
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
            "Domain and port exceptions reside in the layer whose failure they name",
            "The base type states which layer owns the failure; declaring it elsewhere puts the"
                + " vocabulary of that layer outside it and lets an adapter invent failures the"
                + " application never reports",
            arch -> {
              CollectedViolations collected =
                  CollectedViolations.withHeader(
                      "An exception resides outside the layer its base type names");
              collected.addAll(
                  classes()
                      .that(arch.layout().markers().is("domainException"))
                      .and()
                      .resideOutsideOfPackages(
                          arch.layout().markers().declaringPackagePatterns().toArray(String[]::new))
                      .should(LayerConditions.be(arch.inDomain()))
                      .allowEmptyShould(true),
                  arch.classes(),
                  "a domain exception belongs to the domain layer");
              collected.addAll(
                  classes()
                      .that(arch.layout().markers().is("portException"))
                      .and()
                      .resideOutsideOfPackages(
                          arch.layout().markers().declaringPackagePatterns().toArray(String[]::new))
                      .should(LayerConditions.be(arch.inApplication()))
                      .allowEmptyShould(true),
                  arch.classes(),
                  "a port exception - use-case or output-port - belongs to the application layer");
              collected.throwIfAny();
            })
        .selecting(
            "Classes anywhere on the classpath under scan that are assignable to the domain-exception"
                + " or the port-exception role, except the vocabulary's own code: the packages the"
                + " configured role types live in are excluded, so a project's own base class is not"
                + " reported as residing outside a layer it never claimed.")
        .checking(
            "A subtype of DomainException resides in <module>.domain.. of some module root, a"
                + " subtype of PortException - UseCaseException, OutputPortException and any other"
                + " port failure - in <module>.application... An output port's failure is declared"
                + " beside the port, even though the adapter that implements the port raises it."
                + " Both findings are"
                + " collected into one violation. Which of the two a given failure should have been"
                + " is not checked here. An empty selection passes.");
  }

  public DcaRule applicationExceptionsExtendUseCaseException() {
    return DcaRule.of(
            "DCA-ERR-003",
            "Exceptions declared in the application layer must extend UseCaseException or"
                + " OutputPortException",
            "A use case that cannot serve a request reports why through its type, so that one"
                + " incoming adapter can map each outcome onto its protocol and another adapter can"
                + " map the same outcome onto a different one; an output port that declares a"
                + " failure in its contract names it the same way, so the use case can catch exactly"
                + " that outcome",
            arch ->
                classes()
                    .that(arch.inApplication())
                    .and()
                    .areAssignableTo(Throwable.class)
                    .and(DescribedPredicate.not(arch.layout().markers().is("domainException")))
                    .should(
                        LayerConditions.be(
                            arch.layout()
                                .markers()
                                .is("useCaseException")
                                .or(arch.layout().markers().is("outputPortException"))))
                    .allowEmptyShould(true))
        .selecting(
            "Classes in <module>.application.. of every module root that are assignable to Throwable"
                + " and not to DomainException - the exception types the application layer declares"
                + " itself. The base types live in the building blocks and are not selected. A"
                + " subtype of DomainException declared in an application package is"
                + " not selected either: it is misplaced rather than mis-based, and DCA-ERR-002"
                + " owns that case with the remedy that fits it - move the failure to the domain,"
                + " do not change its base type.")
        .checking(
            "Each extends UseCaseException - a failure an input port declares, raised by the use"
                + " case - or OutputPortException - a failure an output port declares, raised by its"
                + " adapter - directly or through an intermediate base class. Extending PortException"
                + " or InputPortException directly does not count: they classify, they are not"
                + " extended by a project. An empty selection passes.");
  }

  public DcaRule exceptionsCarryNoFrameworkMetadata(DcaLayout layout) {
    return DcaRule.check(
            "DCA-ERR-004",
            "Domain and port exceptions must not carry prohibited framework metadata",
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
                + " to PortException - UseCaseException and OutputPortException included - the"
                + " vocabulary's own packages excluded.")
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
            "Domain and port exception names must stay in the language of their layer",
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
                + " to PortException - UseCaseException and OutputPortException included - the"
                + " vocabulary's own packages excluded.")
        .checking(
            "The simple name ends with none of Error, Fault, Failure and contains none of Http,"
                + " StatusCode, ResponseStatus, ResponseEntity. The bare words Status and"
                + " Response are not matched: they are transport words and domain words, and"
                + " the name alone does not tell them apart, so OrderStatusInvalidException"
                + " passes. Whether the remaining name is a term of the Ubiquitous Language is"
                + " not decidable here and stays with review. An empty selection passes.");
  }

  public DcaRule adaptersWithoutTranslation() {
    return DcaRule.informational(
            "DCA-ERR-006",
            "Diagnostic: incoming adapter packages that drive a use case without translating its"
                + " failures",
            "A package that drives the application and knows neither failure type nor a case of"
                + " a closed result either lets everything escape to a generic handler or catches a"
                + " generic type and answers every outcome the same way; whether it does is not"
                + " visible in the import model. A use case may return its expected refusals as"
                + " cases of a closed result instead of throwing them; a switch over those cases is"
                + " the translation then. An adapter that names the abstract base of the port"
                + " failures catches them generically and answers every outcome the same way",
            arch -> {
              String domainException = arch.layout().markers().domainException();
              String useCaseException = arch.layout().markers().useCaseException();
              boolean anyFailureType =
                  arch.classes().stream()
                      .anyMatch(
                          type ->
                              !type.getName().equals(domainException)
                                  && !type.getName().equals(useCaseException)
                                  && (arch.layout().markers().is("domainException").test(type)
                                      || arch.layout()
                                          .markers()
                                          .is("useCaseException")
                                          .test(type)));
              if (!anyFailureType) {
                return List.of(
                    "no class of the scan is assignable to "
                        + domainException
                        + " or "
                        + useCaseException
                        + ", so no package can be seen to name a failure - nothing is listed."
                        + " Where the project has failure types of its own, point the"
                        + " domainException and useCaseException roles at their base types"
                        + " (DcaMarkers.withDomainException / withUseCaseException).");
              }
              List<String> observed = new ArrayList<>();
              DcaMarkers markers = arch.layout().markers();
              Set<String> genericBases =
                  Set.of(
                      markers.portException(),
                      markers.inputPortException(),
                      markers.outputPortException());
              for (JavaClass type : arch.classes()) {
                if (!arch.inIncomingAdapter().test(type)) {
                  continue;
                }
                for (String base : genericBases.stream().sorted().toList()) {
                  if (namesAnyOf(type, Set.of(base))) {
                    observed.add(
                        type.getName()
                            + ": names the abstract base "
                            + base.substring(base.lastIndexOf('.') + 1)
                            + " - catch the named failures, each with its own answer");
                  }
                }
              }
              Set<String> resultCases = casesOfClosedResults(arch);
              Map<String, List<JavaClass>> drivingByPackage = new TreeMap<>();
              Set<String> packagesThatName = new HashSet<>();
              for (JavaClass type : arch.classes()) {
                if (!arch.inIncomingAdapter().test(type)) {
                  continue;
                }
                if (dependsOnAssignableTo(type, arch.layout().markers().is("domainException"))
                    || dependsOnAssignableTo(type, arch.layout().markers().is("useCaseException"))
                    || namesAnyOf(type, resultCases)) {
                  packagesThatName.add(type.getPackageName());
                }
                if (!type.isInterface()
                    && dependsOnAssignableTo(type, arch.layout().markers().is("inputPort"))) {
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
                    observed.add(
                        pkg
                            + ": drives an input port and names neither a failure type of the inner"
                            + " layers nor a case of a closed result ("
                            + driving.stream().map(JavaClass::getSimpleName).sorted().toList()
                            + ")");
                  });
              return observed;
            })
        .selecting(
            "Incoming adapter packages of every module root (<module>.adapter.incoming..) that hold"
                + " at least one non-interface class depending on a class assignable to InputPort -"
                + " the packages that drive the application.")
        .checking(
            "Informational diagnostic only: when no class of the scan is assignable to"
                + " DomainException or UseCaseException, it lists no package and says once that it"
                + " cannot see the failures; otherwise it lists a package when no class in it - a"
                + " central exception handler beside the adapters included - depends on a class"
                + " assignable to DomainException or UseCaseException, nor refers to a case of a"
                + " closed result, and never fails. A closed result is a sealed type that a method"
                + " of an input port returns or that an input port's use-case interface binds as a"
                + " type argument; its cases are its permitted subclasses, recursively. A reference"
                + " to a case is read from the class file's constant pool as well as from the"
                + " import model, so a pattern-matching switch whose arms never touch the bound"
                + " case is seen. This is an approximation of an exhaustive switch: one reference"
                + " to one case counts, whether it is a switch arm, a type test, a cast or a"
                + " construction, and whether the switch covers every case is left to the compiler."
                + " A handler in a different package of the same module is not seen, and a caught"
                + " type is not visible to the import model, so this does not establish that a"
                + " package translates nothing. Beside the packages it lists every incoming-adapter"
                + " class that refers - through the import model or its class file's constant pool,"
                + " where a catch clause's type is recorded - to the abstract bases PortException,"
                + " InputPortException or OutputPortException themselves: it catches or tests the"
                + " port failures generically.");
  }

  public DcaRule failuresCarryIdsAndFigures() {
    return DcaRule.informational(
            "DCA-ERR-007",
            "Diagnostic: failures that carry display data or objects instead of ids and figures",
            "A failure carries ids and figures, never what a page shows. Its fields are the typed"
                + " identities and the values the rule compared; a name or a description of the"
                + " thing an id refers to is resolved by the translation site through the queries"
                + " it already has. A failure that carries the name decides for every surface what"
                + " it shows, and one that carries an aggregate or an entity hands the model out"
                + " through the error path",
            arch -> {
              List<String> observed = new ArrayList<>();
              DcaMarkers markers = arch.layout().markers();
              for (JavaClass failure : arch.classes()) {
                if (failure.isInterface() || !isProjectException(failure, markers)) {
                  continue;
                }
                for (JavaField field : TypeInspection.instanceFields(failure)) {
                  JavaClass owner = field.getOwner();
                  if (field.getModifiers().contains(JavaModifier.SYNTHETIC)
                      || owner.getPackageName().startsWith("java.")
                      || markers.declaresTypesIn(owner.getPackageName())) {
                    continue;
                  }
                  for (JavaClass involved : TypeInspection.involvedTypes(field, failure)) {
                    String finding = fieldFinding(involved, arch);
                    if (finding != null) {
                      observed.add(
                          failure.getName()
                              + "."
                              + field.getName()
                              + " is "
                              + finding
                              + " ("
                              + involved.getSimpleName()
                              + ") - carry the id or the figure the rule compared");
                    }
                  }
                }
              }
              return observed;
            })
        .experimental()
        .selecting(
            "Concrete project classes assignable to DomainException or PortException - the"
                + " building blocks' own types excluded - and their instance fields, inherited"
                + " ones declared in project classes included.")
        .checking(
            "Informational diagnostic only: lists every field that involves - through its raw type"
                + " or a type argument - a plain string, an aggregate or entity, or any other type"
                + " that is neither assignable to the id or value role, nor a number, a primitive,"
                + " an enum or a date and time type of the platform, and never fails. Container"
                + " types such as lists are looked through. The message the base type carries is"
                + " not a field of the project's failure and is not listed. A string that holds a"
                + " code rather than display text is listed as well: the type does not tell the"
                + " two apart, and the remedy is a typed value for the code.");
  }

  public DcaRule closedResultsRefuse() {
    return DcaRule.informational(
            "DCA-ERR-008",
            "Diagnostic: closed use-case results without a case marked as a refusal",
            "A closed result describes the outcomes a use case can answer with; some of them are"
                + " refusals, some are not. A result may hold several outcomes of which none"
                + " refuses - found or nothing found, sent or already sent - and that alone is"
                + " reason enough to close it. The refusal marker on a case says the request was"
                + " refused, a meaning the caller answers with or without a transaction, and it is"
                + " what lets the transaction boundary roll a refusal back like a thrown failure."
                + " Whether a case refuses is domain knowledge the type does not show, so a closed"
                + " result without any marked case is listed for a look, not failed: if one of its"
                + " outcomes is a refusal and stays unmarked, the transaction commits the writes"
                + " made before it",
            arch -> {
              DcaMarkers markers = arch.layout().markers();
              DescribedPredicate<JavaClass> refusal = markers.is("refusal");
              List<String> observed = new ArrayList<>();
              for (Map.Entry<JavaClass, Set<JavaClass>> result : closedResultsOf(arch).entrySet()) {
                JavaClass type = result.getKey();
                if (!arch.inApplication().test(type)) {
                  continue;
                }
                Set<JavaClass> cases = new LinkedHashSet<>();
                collectCaseClasses(type, cases);
                if (cases.stream().anyMatch(refusal::test)) {
                  continue;
                }
                observed.add(
                    type.getName()
                        + " is answered by "
                        + result.getValue().stream().map(JavaClass::getSimpleName).sorted().toList()
                        + " and none of its cases "
                        + cases.stream().map(JavaClass::getSimpleName).sorted().toList()
                        + " is marked as a refusal ("
                        + markers.marker("refusal")
                        + ") - check: is none of these outcomes a refusal? If one is, mark it,"
                        + " or the transaction commits when it is returned");
              }
              return observed;
            })
        .experimental()
        .selecting(
            "Closed results in <module>.application.. of every module root: a sealed type that a"
                + " public method of a type assignable to InputPort returns, or that such an input"
                + " port's use-case interface binds as a type argument - commands and queries"
                + " excluded.")
        .checking(
            "Informational diagnostic only: lists every closed result none of whose cases is"
                + " assignable to the configured refusal role (by default the Refusal marker of"
                + " the building blocks), and never fails. Its cases are its permitted subclasses,"
                + " recursively through sealed cases, so refusals grouped below a sealed case of"
                + " their own count. The result type itself carrying the marker does not count. A"
                + " listed result is not a finding by itself: a closed result whose outcomes are"
                + " all successes - found or nothing found - is correct as it is. A context that"
                + " throws its refusals has no closed results and lists nothing.");
  }

  public DcaRule domainExceptionsConstructedInTheDomain() {
    return DcaRule.check(
            "DCA-ERR-009",
            "A domain exception is constructed only in the domain layer",
            "The model refuses: a domain exception names a business rule the model itself broke"
                + " off. A use case never invents one. A failure the use case finds - an output"
                + " port returned nothing, the caller does not own what it names, a precondition"
                + " on a second aggregate - is the application's, a case of its result or a"
                + " use-case exception; dressed as a domain failure it claims a rule the model"
                + " never checked, and the model cannot know what a search found",
            arch -> {
              DcaMarkers markers = arch.layout().markers();
              DescribedPredicate<JavaClass> domainException = markers.is("domainException");
              DescribedPredicate<JavaClass> inDomain = arch.inDomain();
              List<String> vocabulary =
                  markers.declaringPackagePatternsOf(List.of("domainException"));
              CollectedViolations collected =
                  CollectedViolations.withHeader(
                      "A domain exception is constructed outside the domain layer");
              Set<String> seen = new LinkedHashSet<>();
              for (JavaClass origin : arch.classes()) {
                if (inDomain.test(origin)
                    || vocabulary.stream()
                        .anyMatch(
                            pattern ->
                                PackageMatcher.of(pattern).matches(origin.getPackageName()))) {
                  continue;
                }
                List<JavaCodeUnitAccess<?>> constructions = new ArrayList<>();
                constructions.addAll(origin.getConstructorCallsFromSelf());
                constructions.addAll(origin.getConstructorReferencesFromSelf());
                for (JavaCodeUnitAccess<?> construction : constructions) {
                  JavaClass target = construction.getTargetOwner();
                  if (!domainException.test(target) || chainsTo(construction, target)) {
                    continue;
                  }
                  String finding =
                      construction.getOrigin().getFullName()
                          + " constructs "
                          + target.getName()
                          + " "
                          + construction.getSourceCodeLocation()
                          + " - only the domain layer raises a domain failure; a failure the"
                          + " application finds is a case of its result or a use-case exception";
                  if (seen.add(finding)) {
                    collected.add(finding);
                  }
                }
              }
              collected.throwIfAny();
            })
        .experimental()
        .selecting(
            "Every class of the scan outside <module>.domain.. of every module root - the"
                + " application layer, the adapters, configuration and any unplaced class - with"
                + " each constructor call and constructor reference it makes, lambdas, anonymous"
                + " and nested classes included. Classes in the package of the configured"
                + " domain-exception role are library code and not selected; test code is not"
                + " imported.")
        .checking(
            "No such call or reference creates a type assignable to the configured"
                + " domain-exception role (by default DomainException of the building blocks). A"
                + " subclass constructor chaining to its parent's constructor is not a"
                + " construction - where a misplaced exception type lives is DCA-ERR-002's"
                + " finding. Construction in the domain layer passes, a domain service and a"
                + " factory included. A domain exception the application catches, wraps or"
                + " rethrows is not constructed and passes. A construction through reflection or"
                + " a generic factory method outside the domain is not seen. An empty selection"
                + " passes.",
            "let the aggregate, value object or domain service that checks the rule throw it;"
                + " for a failure the use case finds itself - nothing found, not owned - return a"
                + " refusal case of the result or throw a use-case exception declared beside the"
                + " use case");
  }

  /** A subclass constructor's call to its parent's constructor, or a {@code this(...)} call. */
  private static boolean chainsTo(JavaCodeUnitAccess<?> construction, JavaClass target) {
    return construction.getOrigin().isConstructor()
        && construction.getOriginOwner().isAssignableTo(target.getName());
  }

  /** What is wrong with a type a failure's field involves, or {@code null} when it is fine. */
  private static String fieldFinding(JavaClass type, DcaArchitecture arch) {
    DcaMarkers markers = arch.layout().markers();
    String name = type.getName();
    if (markers.is("aggregateRoot").test(type) || markers.is("entity").test(type)) {
      return "an aggregate or entity";
    }
    if (type.isAssignableTo(CharSequence.class)) {
      return "a plain string";
    }
    if (type.isPrimitive()
        || type.isEnum()
        || type.isAssignableTo(Number.class)
        || markers.is("id").test(type)
        || markers.is("value").test(type)
        || name.equals("java.lang.Boolean")
        || name.equals("java.lang.Character")
        || type.getPackageName().equals("java.time")
        || arch.layout().timestampTypes().contains(name)
        || type.getPackageName().equals("java.util")
        || name.equals("java.lang.Object")) {
      return null;
    }
    return "neither an id, a value nor a number";
  }

  private static final List<String> FORBIDDEN_SUFFIXES = List.of("Error", "Fault", "Failure");

  private static final List<String> FORBIDDEN_WORDS =
      List.of("Http", "StatusCode", "ResponseStatus", "ResponseEntity");

  /** A project's own exception type: assignable to a base type, outside the building blocks. */
  private static boolean isProjectException(JavaClass type, DcaMarkers markers) {
    return (markers.is("domainException").test(type) || markers.is("portException").test(type))
        && !markers.declaresTypesIn(type.getPackageName());
  }

  /**
   * The names of the cases of every closed result an input port answers with: a sealed type a
   * port's method returns or its use-case interface binds as a type argument, and that type's
   * permitted subclasses, recursively through sealed cases.
   */
  private static Set<String> casesOfClosedResults(DcaArchitecture arch) {
    DescribedPredicate<JavaClass> inputPort = arch.layout().markers().is("inputPort");
    Set<String> cases = new HashSet<>();
    for (JavaClass port : arch.classes()) {
      if (!port.isInterface() || !inputPort.test(port)) {
        continue;
      }
      for (JavaClass output : TypeInspection.outputTypes(port, inputPort)) {
        if (output.isSealed()) {
          collectCases(output, cases);
        }
      }
    }
    return cases;
  }

  /**
   * Every closed result an input port answers with, mapped to the ports that answer with it: a
   * sealed type a port's public method returns or its use-case interface binds as a type argument,
   * commands and queries excluded.
   */
  private static Map<JavaClass, Set<JavaClass>> closedResultsOf(DcaArchitecture arch) {
    DcaMarkers markers = arch.layout().markers();
    DescribedPredicate<JavaClass> inputPort = markers.is("inputPort");
    Map<JavaClass, Set<JavaClass>> results =
        new TreeMap<>(java.util.Comparator.comparing(JavaClass::getName));
    for (JavaClass port : arch.classes()) {
      if (!port.isInterface() || !inputPort.test(port)) {
        continue;
      }
      for (JavaClass output : TypeInspection.outputTypes(port, inputPort)) {
        if (output.isSealed()
            && !markers.is("command").test(output)
            && !markers.is("query").test(output)) {
          results.computeIfAbsent(output, key -> new LinkedHashSet<>()).add(port);
        }
      }
    }
    return results;
  }

  private static void collectCaseClasses(JavaClass sealed, Set<JavaClass> cases) {
    for (JavaClass permitted : sealed.getPermittedSubclasses().orElse(Set.of())) {
      if (cases.add(permitted) && permitted.isSealed()) {
        collectCaseClasses(permitted, cases);
      }
    }
  }

  private static void collectCases(JavaClass sealed, Set<String> cases) {
    for (JavaClass permitted : sealed.getPermittedSubclasses().orElse(Set.of())) {
      if (cases.add(permitted.getName()) && permitted.isSealed()) {
        collectCases(permitted, cases);
      }
    }
  }

  /**
   * Whether the class refers to one of the named classes — through the import model, or through its
   * class file's constant pool, which also holds the case types of a pattern-matching switch that
   * the import model leaves out.
   */
  private static boolean namesAnyOf(JavaClass type, Set<String> names) {
    if (names.isEmpty()) {
      return false;
    }
    if (type.getDirectDependenciesFromSelf().stream()
        .anyMatch(dependency -> names.contains(dependency.getTargetClass().getName()))) {
      return true;
    }
    return ClassFileReferences.of(type)
        .map(referenced -> referenced.stream().anyMatch(names::contains))
        .orElse(false);
  }

  private static boolean dependsOnAssignableTo(
      JavaClass type, DescribedPredicate<JavaClass> target) {
    return type.getDirectDependenciesFromSelf().stream()
        .anyMatch(dependency -> target.test(dependency.getTargetClass()));
  }
}
