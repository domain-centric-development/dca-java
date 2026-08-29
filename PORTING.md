# Porting the sample's Groovy/Spock ArchUnit tests to the `dca-archunit` rule library

Working spec for the port. Source: `../dca-ecommerce-sample/src/test-architecture/groovy/de/sample/aiarchitecture/*.groovy`
(read `BaseArchUnitTest.groovy` first — every helper it offers has a counterpart below).

## Target model (already implemented in `dca-archunit/src/main/java/dev/domaincentric/dca/archunit/`)

| Groovy | Java library |
|---|---|
| `BaseArchUnitTest` constants (`BASE_PACKAGE`, `*_SUBPKG`, `*_SUFFIX`, `THIRD_PARTY_PACKAGES_ALLOWED_IN_DOMAIN`) | `DcaLayout` — `layout.basePackage()`, `layout.domainSubpackage()`, `layout.useCaseSuffix()`, `layout.thirdPartyPackagesAllowedInDomain()` … |
| `DOMAIN_PACKAGE`, `APPLICATION_PACKAGE`, `ADAPTER_PACKAGE`, `INCOMING_ADAPTER_PACKAGE`, `OUTGOING_ADAPTER_PACKAGE`, `SHARED_OUTPUT_PORT_PACKAGE`, `INFRASTRUCTURE_PACKAGE`, `SHAREDKERNEL_*` | `layout.domainPattern()`, `layout.applicationPattern()`, `layout.adapterPattern()`, `layout.incomingAdapterPattern()`, `layout.outgoingAdapterPattern()`, `layout.sharedOutputPortPattern()`, `layout.infrastructurePattern()`, `layout.sharedKernelPattern()`, `layout.sharedKernelDomainPattern()`, `layout.sharedKernelDomainModelPattern()` |
| `SHAREDKERNEL_MARKER_*_PACKAGE` (the sample's own marker package — **gone** after the port) | `DcaLayout.BUILDING_BLOCKS_PACKAGE`, `BUILDING_BLOCKS_TACTICAL_PACKAGE`, `BUILDING_BLOCKS_STRATEGIC_PACKAGE`, `BUILDING_BLOCKS_PORT_PACKAGE`, `BUILDING_BLOCKS_PORT_IN_PACKAGE`, `BUILDING_BLOCKS_PORT_OUT_PACKAGE` |
| `*_MARKER`, `*_ANNOTATION` class constants | the building-blocks types directly: `dev.domaincentric.dca.buildingblocks.ddd.tactical.*`, `ddd.strategic.BoundedContext`, `ddd.strategic.relationships.{SharedKernel,OpenHostService,Upstream,Upstreams,ExternalUpstream,ExternalUpstreams,Partnership,Partnerships}`, `hexagonal.port.in.{InputPort,UseCase}`, `hexagonal.port.out.{OutputPort,Repository,Store,DomainEventPublisher,IntegrationEventPublisher}` |
| `allClasses` | `arch.classes()` (`DcaArchitecture arch`) |
| `discoverBoundedContextPackages()` | `arch.boundedContexts()` (`Map<String, BoundedContext>`), `arch.boundedContextPackages()` |
| `discoverSharedKernelPackage()` | `arch.sharedKernelPackage()` (`Optional<String>`) |
| `getPackageAnnotation(pkg, T)` / `getPackageAnnotations(pkg, T)` | `arch.packageAnnotation(pkg, T.class)` (`Optional<T>`) / `arch.packageAnnotations(pkg, T.class)` (`List<T>`) |
| `extractRootContextPackage(pkg)` | `arch.rootContextPackage(pkg)` |
| `getBoundedContextPackagePatterns()` / `…Array()` / `…Excluding(p)` | `arch.boundedContextPatterns()` / `arch.boundedContextPatternsExcluding(p)` |
| `getBoundedContextDomainPatterns()` etc. | `arch.contextDomainPatterns()`, `arch.contextDomainModelPatterns()`, `arch.contextApplicationPatterns()`, `arch.contextAdapterPatterns()` |
| `allIncomingAdapterPatterns()` / `allOutgoingAdapterPatterns()` | same names on `arch` |
| `allDomainPatternsWithSharedKernel()` / `allDomainModelPatternsWithSharedKernel()` | same names on `arch` |
| `INFRASTRUCTURE_IMPLEMENTATION` | `arch.infrastructureImplementation()` |
| Spring annotation classes (`Service`, `Component`, `Controller`, `RestController`, `Transactional`, `EventListener`, Modulith `ApplicationModule`) | **by name** — `layout.frameworkAnnotations().service()` etc. with ArchUnit's `annotatedWith(String fqn)` / `isAnnotatedWith(String)`. The library has **no** Spring dependency. |
| `org.springframework.modulith.core.ApplicationModules` (Modulith verification) | **not ported** — stays in the sample (`SpringModulithVerificationTest`). |

## Rule shape

Each Spock feature `def "<title>"() { … }` becomes one `DcaRule`:

```java
DcaRule.of("DCA-TAC-001", "Aggregate roots must implement AggregateRoot",
    "Aggregate roots are the consistency boundary — the marker is what the rules pin to",
    arch -> classes().that()....should()....)          // returns ArchRule; .as()/.because()/.check() added by the factory
```

or, for features that loop over contexts / do reflective checks / run several ArchUnit rules:

```java
DcaRule.check("DCA-MAP-007", "Implemented Upstream declarations must be backed by an actual code dependency",
    "A declaration without a dependency is stale — mark it PLANNED or remove it",
    arch -> { for (String ctx : arch.boundedContextPackages()) { … .check(arch.classes()); } })
```

Rules:

- **Title** = the Spock feature name verbatim (it is the knowledge-catalog key). Trim only a leading category prefix if the Groovy title has one.
- **Rationale** = the `.because(...)` text if the feature has exactly one; otherwise write one sentence that states *why* (never *what*). In `DcaRule.check` bodies, keep every inner `.because(...)` as it was.
- **Id** = `DCA-<SET>-<NNN>`, numbered in the order the features appear in the Groovy file, starting at 001. Sets: `TAC` (DddTacticalPatterns), `STR` (DddStrategicPatterns), `MAP` (ContextMap), `ADV` (DddAdvancedPatterns), `HEX` (HexagonalArchitecture), `LAY` (LayeredArchitecture), `ONI` (OnionArchitecture), `NAM` (NamingConventions), `USE` (UseCasePatterns), `CYC` (PackageCycles).
- Features whose only assertion is `true` (documentation-only, "Diagnostic") become a `DcaRule.check` that prints/asserts nothing harmful — keep them, mark title with `Diagnostic:` as in the source, they must never fail.
- Rule sets implement `DcaRuleSet` in package `dev.domaincentric.dca.archunit.rules`, class name `<Set>Rules` (`TacticalPatternRules`, `StrategicPatternRules`, `ContextMapRules`, `AdvancedPatternRules`, `HexagonalRules`, `LayeredRules`, `OnionRules`, `NamingRules`, `UseCaseRules`, `CycleRules`), constructor `(DcaLayout layout)`, `name()` returns the lower-case set name (`"tactical"`, `"strategic"`, `"contextmap"`, `"advanced"`, `"hexagonal"`, `"layered"`, `"onion"`, `"naming"`, `"usecase"`, `"cycles"`), `rules()` returns an immutable `List<DcaRule>` built once in the constructor. One `public static` accessor per rule is welcome but optional.
- No hard-coded package names from the sample (`de.sample`, `aiarchitecture`, `sharedkernel.marker`, `backoffice`, `product`, `cart` …). Everything comes from `DcaLayout` / `DcaArchitecture`. If a Groovy rule really depends on a sample-specific package (e.g. `backoffice`), make it a `DcaLayout`-independent rule over the discovered contexts, or drop it and note the drop in `PORTING-LOG.md`.
- Keep the `.because()` wording; it is doctrine text. Fix obvious grammar only.
- Known defects to fix while porting (from the sample's `TODO.md`): Entity/AggregateRoot setter rule must use `getAllMethods()` like the Value rule; the "Enriched Domain Models must be Value Object records" rule must also check `areAssignableTo(Value.class)`; repository↔aggregate matching must use fully-qualified names (same context package), and must **fail** when the referenced aggregate cannot be resolved. Note each fix in `PORTING-LOG.md`.
- Java 17 source level (records, text blocks, switch expressions, `instanceof` patterns OK; no sealed-pattern-matching in switch, no `_` unnamed variables). Google Java Format — run `./gradlew :dca-archunit:spotlessApply`.

## Fixtures and self-tests

Every rule set gets its own fixture tree under `dca-archunit/src/test/java/dev/domaincentric/dca/archunit/fixtures/<set>/`:

```
fixtures/<set>/good/            ← a tiny DCA-conformant code base: base package = …fixtures.<set>.good
  sharedkernel/package-info.java   @SharedKernel
  sharedkernel/domain/model/…      shared value objects if needed
  <ctx>/package-info.java          @BoundedContext(name=…, description=…)
  <ctx>/domain/model/…             aggregate, entity, value, id, domain event
  <ctx>/application/<usecase>/…    *InputPort, *UseCase, *Command/*Query, *Result
  <ctx>/application/shared/…       *Repository extends Repository<…>
  <ctx>/adapter/incoming/…  <ctx>/adapter/outgoing/…
fixtures/<set>/bad/             ← same shape, base package …fixtures.<set>.bad, with one violation per rule
```

Fixture classes implement the real building-blocks markers. They have no framework dependency;
where a rule needs a framework annotation (e.g. "use cases must be @Service"), declare a **local
annotation in the fixture tree with the same FQN the layout expects** — e.g. create
`fixtures/<set>/good/…` classes annotated with an annotation you define in package
`org.springframework.stereotype` under `src/test/java` (`Service.java` with `@Retention(RUNTIME)`). Only one agent may create each such shim: the **Hexagonal/Layered/Onion** port owns `org.springframework.stereotype.{Service,Component,Controller}`, `org.springframework.web.bind.annotation.RestController`, `org.springframework.transaction.annotation.Transactional`, `org.springframework.context.event.EventListener`, `org.springframework.modulith.ApplicationModule`; everyone else uses them (check the directory first; if missing, create only what you need and say so in your report).

Self-test class `dca-archunit/src/test/java/dev/domaincentric/dca/archunit/rules/<Set>RulesTest.java` (JUnit 5):

```java
static DcaArchitecture arch(String pkg) {
  return DcaArchitecture.of(DcaLayout.forBasePackage(pkg),
      new ClassFileImporter().importPackages(pkg));     // NOT DcaArchitecture.load — that excludes test output
}
@TestFactory Stream<DynamicTest> goodFixturePasses()  // every rule passes on …good
@TestFactory Stream<DynamicTest> badFixtureFails()    // every rule that has a negative fixture throws AssertionError on …bad
```

Every rule needs a passing case. Every rule should have a failing case; where a negative fixture is
impossible or absurd, list the rule id in a `NO_NEGATIVE_FIXTURE` set with a one-line reason.

`package-info.java` in fixtures must be real (the discovery reads `Class.forName(pkg + ".package-info")`).

## Compile / test

```
./gradlew :dca-archunit:spotlessApply :dca-archunit:test --tests '*<Set>RulesTest*' -q
```

Other rule sets are stubs while the port is in flight — a red build from *another* agent's file is not
yours to fix; report it. Do not edit files outside your rule set, fixtures, test class and `PORTING-LOG.md`
(append-only, one `## <Set>` section per agent).
