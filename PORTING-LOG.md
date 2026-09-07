# Porting log

Append-only. One section per rule set: dropped rules, fixed defects, deviations from the Groovy source.

## Advanced

Source: `DddAdvancedPatternsArchUnitTest.groovy` → `AdvancedPatternRules` (`DCA-ADV-001` … `DCA-ADV-018`, 18 rules, none dropped).

- **Spring annotations by name.** `@Component`/`@Service`/`@EventListener` checks use `layout.frameworkAnnotations()` (`beAnnotatedWith(String)`); the library has no Spring dependency.
- **`..domain.service..` derived from the layout.** ADV-009 builds the package pattern from `layout.domainSubpackage()` instead of the literal `"..domain.service.."`.
- **`allowEmptyShould(true)` added to ADV-001 and ADV-009.** The Groovy features would fail on a code base without any domain event / domain service; a library rule must be a no-op there. Groovy features that already had it keep it.
- **Rationale for the three reflective checks (ADV-006/007/008).** The Groovy versions have no `.because()`; one *why* sentence was written for each. The original `AssertionError` header and per-class messages are kept verbatim in the check bodies.
- **Shims created here:** `org.springframework.stereotype.{Service,Component}` under `dca-archunit/src/test/java` (the test tree was empty when this port started; the Hexagonal port owns them — content is a trivial `@Retention(RUNTIME) @Target(TYPE)` annotation, safe to overwrite).

## Cycles

Source: `PackageCyclesArchUnitTest.groovy` → `rules/CycleRules.java` (DCA-CYC-001…004, `name()` = `"cycles"`).

- All four features ported 1:1 as `slices().matching(...).should().beFreeOfCycles()`; slice patterns built from `DcaLayout` (`basePackage`, `domainSubpackage`, `applicationSubpackage`, `adapterSubpackage`, `incomingSubpackage`/`outgoingSubpackage`) instead of the hard-coded `domain.model` / `application` / `adapter.outgoing` / `adapter.incoming` segments. The `.model` sub-segment of the domain slice stays literal (no layout knob exists for it, consistent with `DcaLayout.domainModelPattern()`).
- `.because()` texts kept verbatim as rationale.
- Fixtures `fixtures/cycles/{good,bad}` use two contexts (`alpha`, `beta`); *good* depends one-way alpha→beta per layer, *bad* has an alpha↔beta cycle in every one of the four layers. Every rule has a negative fixture (`NO_NEGATIVE_FIXTURE` is empty).

## ContextMapRenderer

Source: `ContextMapDocumentationTest.groovy` → `contextmap/ContextMapRenderer.java` (opt-in renderer, **not** a `DcaRule`).

- API: `ContextMapRenderer.of(DcaArchitecture)`, options `includeExternalSystems(boolean)` (default true), `includePlanned(boolean)` (default true), `withMermaid(boolean)` (default true), `withTitle(String)` (default `"Context Map"`); terminals `String render()` and `void writeTo(Path)` (creates parent dirs, overwrites, `UncheckedIOException` on failure).
- The stale-file assertion of the Groovy test (regenerate + fail when changed) is *not* part of the library — a consuming project writes that two-line test itself around `render()`/`writeTo()`.
- Markdown structure and wording are those of the Groovy renderer (contexts table, Mermaid `graph LR`, upstream table, external systems, partnerships, legend). Changed: the generated-from note now names `ContextMapRenderer` and says "regenerate and commit" instead of `./gradlew test-architecture`; the sentence "Non-context modules (e.g. backoffice) …" lost the sample-specific example. The committed `dca-ecommerce-sample-java/docs/context-map.md` is an older hand-written format and was *not* used as the template — the Groovy renderer's output is.
- `includePlanned(false)` drops PLANNED `@Upstream` and `@ExternalUpstream` declarations from tables and diagram; `includeExternalSystems(false)` drops the external-systems section and nodes/edges; `withMermaid(false)` drops the diagram section incl. legend.
- Published-interface detection (`api`/`events` badge) still requires `@NamedInterface` **only when** `org.springframework.modulith.NamedInterface` is on the classpath (loaded reflectively, `value()`/`name()` read via reflection); otherwise class presence in `<ctx>.api`/`<ctx>.events` suffices. No Spring dependency added.
- Fixture `fixtures/contextmaprender/` (catalog, cart, shipping) covers `@Upstream` (API+EVENTS, PLANNED), `@ExternalUpstream` (OUTBOUND/REST), symmetric `@Partnership`, and an `api` package. No framework shims created or needed.

## Hexagonal

- Source: `HexagonalArchitectureArchUnitTest.groovy` → `HexagonalRules` (`DCA-HEX-001`…`010`), all ten features ported, none dropped.
- `.because()` texts that named the sample's marker package (`sharedkernel.marker.port.out`) now say `port.out` — the markers come from dca-building-blocks.
- `DCA-HEX-008` (`*Repository` classes reside in outgoing adapter) gained `allowEmptyShould(true)` so it does not error on a code base without repository implementations yet.
- `DCA-HEX-009` gained `allowEmptyShould(true)` for the same reason (no `application.shared` interfaces yet).
- The `..adapter.incoming.event..` exception pattern is derived from the layout (`adapterSubpackage`/`incomingSubpackage`) instead of hard-coded.
- Fixtures: one shared tree `fixtures/hexagonal/{good,bad}` is used by `HexagonalRulesTest`, `LayeredRulesTest` and `OnionRulesTest` (the three sets check the same dependency directions; the `bad` tree carries one violation per rule of all three sets). No `fixtures/layered` or `fixtures/onion` directories exist.
- Shims created (test-only, by-name mirrors): `org.springframework.stereotype.{Service,Component,Controller}`, `org.springframework.web.bind.annotation.RestController`, `org.springframework.transaction.annotation.Transactional`, `org.springframework.context.event.EventListener`, `org.springframework.modulith.ApplicationModule`.

## Layered

- Source: `LayeredArchitectureArchUnitTest.groovy` → `LayeredRules` (`DCA-LAY-001`…`005`).
- `DCA-LAY-001` was `true`-only in the source (ArchUnit `layeredArchitecture()` contradicts Ports & Adapters); kept as `Diagnostic:` no-op rule, no negative fixture.
- `DCA-LAY-004`: `@Transactional` matched by name via `layout.frameworkAnnotations().transactional()`; the outgoing-adapter pattern is derived from the layout instead of the literal `..adapter.outgoing..`.
- `DCA-LAY-005`: scoped to `DcaLayout.BUILDING_BLOCKS_PORT_OUT_PACKAGE` instead of the sample's `sharedkernel.marker.port.out`. No negative fixture — the markers are library code and not part of the fixture import (rule is vacuous there, `allowEmptyShould(true)`).

## Onion

- Source: `OnionArchitectureArchUnitTest.groovy` → `OnionRules` (`DCA-ONI-001`…`003`).
- `DCA-ONI-002` used its own `ClassFileImporter` in the source; it now runs on `arch.classes()` like every other rule. Allowed marker packages are `BUILDING_BLOCKS_TACTICAL_PACKAGE` and `BUILDING_BLOCKS_PORT_OUT_PACKAGE`; third-party allow-list comes from `layout.thirdPartyPackagesAllowedInDomain()`.
- `DCA-ONI-003`: `@Component`/`@Service` matched by name via `FrameworkAnnotations`; JPA names stay literal as in the source.

## Tactical

Source: `DddTacticalPatternsArchUnitTest.groovy` → `TacticalPatternRules` (22 rules, DCA-TAC-001 … DCA-TAC-022). No rule dropped.

Fixed defects (from the sample's `TODO.md`):

- **DCA-TAC-006** "Domain model classes must not have public setter methods" now iterates `getAllMethods()` (was `getMethods()`), so setters inherited from a superclass are caught — same as the Value Object setter rule. Negative fixture: `bad/…/Shipment extends TrackedEntity`, where only the superclass declares `setNote`.
- **DCA-TAC-016** "Repositories must only exist for Aggregate Roots" resolves the aggregate by simple name **within the repository's own bounded context** (`arch.rootContextPackage(...)` of repository and candidate must match) instead of the first class with that simple name anywhere. A repository whose name cannot be resolved to any class in its context is now a **violation** (was silently accepted). Negative fixtures: `ShipmentRepository` (resolves to an Entity) and `PhantomRepository` (unresolvable).
- **DCA-TAC-022** "Enriched Domain Models must be Value Object records" additionally requires `beAssignableTo(Value.class)`; the `.because()` text already promised this. Negative fixture: `EnrichedOrder` is a record but not a `Value`.

Deviations:

- DCA-TAC-022 gained `allowEmptyShould(true)` (the Groovy rule failed on code bases without any `Enriched*` class — unsuitable for a library).
- Rationales for the custom-check rules (which have no `.because()` in Groovy) are one-sentence "why" texts; the original `AssertionError` message headers are kept verbatim as the violation message.
- DCA-TAC-021 keeps its original full message including the "Fix:" hint.
- No framework annotation shims needed — tactical rules match markers only.

## Naming

Source: `NamingConventionsArchUnitTest.groovy` → `NamingRules` (DCA-NAM-001…011), all 11 features ported 1:1.

- `UseCase`/`Resource` suffixes come from `layout.useCaseSuffix()` / `layout.restControllerSuffix()`; the titles of NAM-001 and NAM-006 embed the configured suffix (identical to the Groovy title for the default layout).
- `@Service`, `@Controller`, `@RestController` matched by name via `layout.frameworkAnnotations()`.
- NAM-006 (REST controllers) and NAM-011 (ViewModels) had no `allowEmptyShould(true)` in Groovy; added, because a library rule must not fail on projects without REST controllers or view models.
- NAM-009 (technical bucket packages): the Groovy `noClasses()` ran over all imported classes; the port restricts `that()` to `basePackage..` so the rule cannot trip over third-party classes resolved from the classpath.
- NAM-011 hard-coded `..adapter.incoming.web..`; the port builds `base.*.<adapter>.<incoming>.web..` from the layout (the `web` segment stays fixed — there is no layout setting for it).

## UseCase

Source: `UseCasePatternsArchUnitTest.groovy` → `UseCaseRules` (DCA-USE-001…011), all 11 features ported 1:1.

- USE-001 checks `DcaLayout.BUILDING_BLOCKS_PORT_IN_PACKAGE` instead of the sample's `sharedkernel.marker.port.in`; title adjusted accordingly ("…must be in the building-blocks port in package").
- USE-006 uses `dev.domaincentric.dca.buildingblocks.ddd.tactical.Value` for the "domain Value Objects named *Result are allowed" exemption.
- USE-009 (publish after save) matches `Repository`/`DomainEventPublisher` from the building blocks; logic unchanged.
- USE-010 (no DTOs in domain) had no `allowEmptyShould(true)` in Groovy; added for symmetry with USE-011 and so the rule set works on code bases with an as yet empty domain layer.
- **USE-012 (added 2026-08-30, not from the Groovy source):** use cases that call the `DomainEventPublisher` must carry `@Transactional` (class or method, meta-annotations included). Spring skips `@TransactionalEventListener`/`@ApplicationModuleListener` silently when no transaction is active and Modulith registers publications in the publishing transaction — a non-transactional publishing use case succeeds while the other contexts never hear of it. Annotation name from `FrameworkAnnotations.transactional()`. Negative fixture: the Bad `PlaceOrderUseCase`; the Good one gained `@Transactional`.
- **USE-012 relaxed / USE-013 added (2026-08-30; `UnitOfWork` renamed to `TransactionBoundary` and moved to `buildingblocks.application` the same day — a transaction boundary is execution semantics, not an output port):** USE-012 accepts an explicit `TransactionBoundary.inTransaction(...)` as the boundary, not only `@Transactional`. USE-013: a `@Transactional` use case (class or method) must not call an output port that may leave the process — any `OutputPort` that is not `Repository`, `Store`, `DomainEventPublisher` or `IntegrationEventPublisher` (`TransactionBoundary` itself is not an output port). Motivation: a remote call inside the transaction holds the connection for the round trip (pool exhaustion) and a rollback cannot undo the remote effect. Use cases with remote reads draw the boundary by hand: remote read → `transactionBoundary.inTransaction(load, mutate, save, publish)`. Inside `run` the rule cannot see the lambda boundary, so it only checks annotated use cases. Fixtures: Good `ShipOrderUseCase` (TransactionBoundary + `CarrierPort` outside), Bad `ShipOrderUseCase` (`@Transactional` + `CarrierPort`).
- Every rule has a negative fixture (`NO_NEGATIVE_FIXTURE` is empty for both sets).
- No files created outside the assigned set; the Spring annotation shims under `src/test/java/org/springframework/**` already existed (Hexagonal agent) and were reused.

## Strategic

Source: `DddStrategicPatternsArchUnitTest.groovy` → `StrategicPatternRules` (`DCA-STR-001`…`010`).

- **Allow-lists dropped as configuration.** `ALLOWED_CROSS_CONTEXT_DEPENDENCIES` and
  `ALLOWED_ADAPTER_CROSS_CONTEXT_ACCESS` were both empty in the source; the port has no
  exception mechanism (STR-003, STR-006). Titles keep the "(except allowed …)" wording because
  they are catalog keys.
- **STR-001 (Diagnostic)** prints contexts and shared kernel only; the Groovy asserts
  `contexts.size() >= 1 && sharedKernel != null` were dropped so the diagnostic never fails.
- **STR-002** is skipped silently when no `@SharedKernel` package exists (Groovy would have
  produced a `null..` pattern).
- **STR-005 / STR-007** derive `..adapter.incoming.openhost..` / `..adapter.outgoing.event..` from
  `DcaLayout` sub-package names; `..api..` and `..events..` stay literal (published-interface
  convention, not a layout setting). Rationale texts drop the "Spring Modulith @NamedInterface"
  mention in favour of "published named interface" — the library is framework-agnostic.
  *Revised 2026-09-03 (WP-21):* `api`/`events` are layout settings now (`DcaLayout.apiSubpackage()`,
  `eventsSubpackage()`), and STR-005 accepts `api/` or **any** sub-package of the incoming adapter —
  an Open Host Service is a relationship pattern, not a folder; `openhost/` is no longer special.
- **STR-010** ("Event Listeners consuming integration events should use Anti-Corruption Layer")
  was `true`-only in the source and stays a never-failing rule without a `Diagnostic:` prefix
  (title verbatim). No negative fixture (STR-001, STR-010).

## ContextMap

Source: `ContextMapArchUnitTest.groovy` → `ContextMapRules` (`DCA-MAP-001`…`013`).

- **MAP-006 (Modulith agreement)** has no Spring dependency: the annotation type is loaded by the
  name in `FrameworkAnnotations.applicationModule()` and `allowedDependencies()` is read via
  reflection. Skipped silently when the name is not configured, the class is not on the class
  path, or the attribute is missing. Semantics unchanged from the source: PLANNED edges also
  have to appear in `allowedDependencies` (both sides describe the same edges).
- **MAP-007 / MAP-008 / MAP-009** skip `@Upstream` declarations whose `context` does not resolve
  to a discovered bounded context; the Groovy code would have built a `null.api..` pattern.
  Dangling references are reported by MAP-004 alone.
- Adapter patterns in MAP-008 / MAP-010 come from `DcaLayout` (`incoming`/`outgoing`
  sub-packages) instead of the literal `adapter.outgoing..` / `adapter.incoming..`.
- **MAP-013 (Diagnostic)** prints the declared map and never fails (source asserted
  `contexts.size() >= 1`). No negative fixture.
- Fixtures use the shim `org.springframework.modulith.ApplicationModule` created by the
  Hexagonal port (not created here).

## Post-port changes

Changes made after the Groovy → Java port, where the ported behaviour itself was revised. Kept here
because the port notes above describe the *old* selection mechanism in several places.

### Context discovery and module discovery (2026-09-02, planning WP-18)

The ported rules selected their classes through `DcaLayout`'s wildcard patterns (`base.*.domain..`),
which is what the Groovy source did. `*` is exactly one segment, so those patterns only ever matched
a bounded context that was a direct child of the base package — a grouped (`base.sales.order`),
nested (`base.contexts.todo`) or flat (context = base package) layout matched no layer rule at all
and passed for lack of subjects.

Two concepts are now separate, and the port notes should be read with that in mind:

- **Bounded context** — declared with `@BoundedContext`, found by walking up from a class's package
  (`DcaArchitecture.rootContextPackage`), at any depth. Drives the strategic and context-map rules.
  Identified by its name relative to the base package (`contextName`), matching Spring Modulith's
  own derivation.
- **Module root** — structural: the shortest package prefix whose remainder starts with a layer
  segment (`DcaArchitecture.moduleRoots()`). Drives the layer, hexagonal, onion, naming, tactical and
  use-case rules, which is why a module that is deliberately not a bounded context stays governed.

The isolation rules (`DCA-STR-003`, `-004`, `-006`, `DCA-HEX-007`) select over module roots as well
(`isolatedModuleRoots()` — every module root except the shared kernel), on the source and on the
target side, with the target's `api`/`events` packages as the only allowed dependency for adapters
(2026-09-03, planning WP-21). So a module needs no declaration of any kind to be governed and
protected; `@BoundedContext` decides context-map membership only. A rule `DCA-LAY-006` demanding such
a declaration existed briefly during WP-18 and was dropped before release — the id is free. The four
rules also collect their per-module violations and throw once, instead of stopping at the first
module (the Groovy originals, one ArchUnit rule per context, had the same first-failure behaviour).

### Features within a bounded context (2026-09-06, planning WP-23)

Two rules that did not exist in the Groovy source, added to the Java library first and ported to .NET
with the same ids, titles and rationales: `DCA-USE-014` (one consistent use-case package depth per
module — `application.<usecase>` or `application.<feature>.<usecase>`) and `DCA-CYC-005` (no cycles
between the immediate child packages of a module's application package, `shared` excepted). Both
select through `moduleRootOf(...)`, so they hold at any module depth. The compatibility fixture
`fixtures.features.compat` runs the whole catalog against a feature-grouped context and is the guard
that no selector regresses to a direct-child assumption. Rule count 110 → 112.

### Shaping the result (2026-09-06, planning WP-24)

Two rules without a Groovy ancestor, added to the Java library first: `DCA-USE-015` (use case
result models must not expose aggregate roots or entities — a transitive field walk through
generic type arguments, nested records and part records anywhere in the application layer
(`application.shared` included), every offending path in
one violation) and `DCA-HEX-012` (incoming adapters must not depend on domain services — event
consumers included, outgoing adapters deliberately outside the selection, because repositories
reconstitute domain objects while implementing output ports). Both are marker-based
(`AggregateRoot`, `Entity`, `DomainService`) and hold at any module depth. Fixtures:
`fixtures.usecase.{good,bad}.order.application.{getorder,listorders}` and a `PricingPolicy`
domain service in `fixtures.hexagonal`. Rule count 112 → 114.

### Empty selections in a greenfield project (2026-09-06, planning WP-06)

Bootstrapping a fresh project through `/dca-bootstrap` (one context, model plus use case, no adapter
yet) surfaced five rules that still relied on ArchUnit's fail-on-empty default: `DCA-HEX-002/004/005/006`
and `DCA-LAY-003`. All five now carry `allowEmptyShould(true)`; `GreenfieldTest` keeps the whole
catalog green against `fixtures.layout.greenfield`. The .NET library passes every empty selection by
construction (`DcaRule.Of`), so nothing changes there. Rule count unchanged (114).

### Review follow-up: complete violation collection, per-method transactions (2026-09-06)

A package review of the checkout at `4a882bd` found ten enforcement defects and three tooling gaps;
`notes/dca-java-review-2026-09-06.md` in the meta-repository carries the dispositions. What changed in
the library, in the order the rules see it:

- Every rule that checks several things collects first and throws once (`CollectedViolations`, now an
  accumulator with `require`/`add`/`addAll`): the twelve context-map rules, `DCA-STR-002`, `DCA-LAY-004`.
  Tolerating one violation cannot pass the rest any more.
- `DCA-USE-009/-012/-013` reason per method along the class-internal call graph (`IntraClassCalls`).
  Documented limit: lambda bodies are attributed to the enclosing method by ArchUnit, so placement
  inside `inTransaction(...)` is not provable statically.
- One type traversal (`TypeInspection.involvedTypes` = ArchUnit's `getAllInvolvedRawTypes()`) for
  `DCA-TAC-003/-007/-008/-017` and `DCA-USE-015`; `DCA-USE-015` walks inherited instance fields and
  reports every path; `DCA-TAC-012` requires `equals(Object)`/`hashCode()` exactly.
- Package selection: infrastructure = global package + every isolated module's `infrastructure` package,
  exact segment boundary (shared kernel excluded — its `infrastructure` is shared support, which the
  reference implementation's `@AsyncInitialize` relies on); `DCA-NAM-011` from module roots; `DCA-MAP-001`
  over every package below the base package.
- `.ignore` values are one regex; indexed keys for several. Node ids via `Locale.ROOT`, one function.
- .NET twin: the same fixes where the defect existed there — `MAP-001` nested namespaces, `USE-015`
  inherited members and per-path reporting, `NAM-011` from module roots, per-module infrastructure,
  indexed `.ignore` keys, `USE-009` rationale. The .NET context-map rules already collected, normalised
  with the invariant culture and matched `Equals(object)` exactly. `USE-012/-013` stay n/a there.

Rule count unchanged (114). Self-tests 319 → 356 (Java, plus 7 in dca-building-blocks) and 326 → 336 (.NET).

### Recheck follow-up: generic inheritance, own-type containers, directed call paths (2026-09-07)

An independent recheck of the review follow-up (`notes/dca-java-recheck-2026-09-07.md` in the
meta-repository) found four remaining defects; all four are fixed the same day, within the abstractions the
follow-up introduced:

- `TypeInspection.involvedTypes(JavaField, JavaClass)` reads an inherited field in the inspected class's
  context — the type arguments of every superclass on the way to the field's owner are substituted, through
  any number of levels and inside containers (`Intermediate<U> extends Base<List<U>>`). One traversal
  (`collect`) serves both the field form and the plain `JavaType` form; `getAllInvolvedRawTypes()` remains
  the leaf. `DCA-TAC-003/-007/-008` and `DCA-USE-015` use the field form.
- `DCA-TAC-003` tolerates exactly the direct field of the own type again (`isSelfReference`); a container
  of the own type is reported, as `List<Order>` was before the traversal refactor — and now also
  `Optional<Order>`, `Order[]`, `Map<String, Order>`, nested lists.
- `IntraClassCalls` lost `connectedTo` (undirected) and gained `reachableFrom`, `entryPointsOf` and
  `reachableThrough(start, predicate)`. `DCA-USE-009` judges every entry path to a saving method;
  `DCA-USE-012` every entry path to a publishing method. Messages name the path (`execute`,
  `executeQuietly (via persist)`). A second recheck the same day (`notes/dca-java-recheck-2-2026-09-07.md`)
  tightened both: an entry point is any unit callable from outside the class (non-private, non-synthetic) or
  one nothing in the class calls — a public method called by a publishing wrapper stays a path of its own;
  and `DCA-USE-012` searches for an *uncovered route* (`reachableThrough` over units without annotation or
  boundary) instead of asking whether *some* unit on *some* route is covered — a boundary on one route to a
  publisher no longer covers a second route. Fixtures `directentry`, `publicwrapper`, `mixeddiamond`,
  `covereddiamond`.
- The lambda-placement limit was re-verified against ArchUnit 1.5.0: `JavaClass.getCodeUnits()` holds no
  `lambda$…` unit, the lambda's calls carry the enclosing method as origin (line numbers only). It stays a
  documented limit; no bytecode analyser was built.

.NET twin: `IntraClassCalls` ported over the runtime type's IL (async state machines and lambda hosts
are units of the graph, joined to their declaring method), `DCA-USE-009` per entry path with the same
fixtures; `DCA-USE-015` already substituted generic bases through reflection; `DCA-TAC-003` already
rejected the own-type list and gained arrays and generic-base members alongside. Rule count unchanged
(114). Self-tests 356 → 367 (Java).

