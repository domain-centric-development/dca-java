# Changelog — dca-archunit

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

## [0.4.0] - 2026-09-10

**Migration from 0.3.0.** Depends on `dca-building-blocks` 0.2.0 (`registerEvent` is protected — see its changelog).
Before 1.0 a minor version may add and tighten rules; what can turn a green 0.3.0 build red:

- `DCA-USE-012` now demands the transaction boundary for every use case that **saves or deletes** an aggregate, not only
  for one that publishes — annotate the class or method, or wrap the work in `TransactionBoundary.inTransaction(...)`.
- `DCA-USE-016` (new): a use case may not invoke another use case — directly, through its input port or through an
  application helper. Extract the shared step into a domain service or an application-layer coordinator.
- `DCA-USE-017` (new): the public surface of a use case is its input port. Public methods of a `*UseCase` that implements
  no `InputPort` are all reported — implement the port.
- `DCA-MAP-008` wants one translation site per declared ACL interaction; `DCA-CYC-005` also sees cycles between use-case
  packages inside one feature.
- Three ids are retired and cannot be selected any more (`onlyIds` fails with the replacement): `DCA-TAC-022`
  (→ `DCA-TAC-014`), `DCA-MAP-003` (renderer disambiguation replaced the rule), `DCA-ADV-003` (→ `DCA-ADV-001`).
- Custom-rule authors: `FrameworkAnnotations.restController()`, `transactional()` and `eventListener()` return
  `List<String>`; the Spring-named accessors are deprecated delegates. `UseCaseRules.publishingUseCasesAreTransactional(layout)`
  is renamed to `mutatingUseCasesAreTransactional(layout)` — the only removed public method; a rule set that built the
  catalog by hand has to follow (`DcaRules.all(layout)` and the set classes are unaffected).

Relaxed at the same time (a red 0.3.0 build may turn green): `DCA-HEX-005` allows global and own-module infrastructure in
outgoing adapters; `DCA-NAM-002` is informational (configuration wiring is as valid as a stereotype); `Manager` is a
valid domain term (`DCA-NAM-010`); a `*Response` may live in an outgoing adapter (`DCA-USE-008`); a Repository or Store
used by one use case may live with it (`DCA-TAC-014/019`); `Store.findById` is allowed (`DCA-TAC-021`); a `version` field
that is a business revision passes `DCA-ADV-006/007`; and `DCA-USE-009` exempts a use case whose aggregate provably never
registers an event.

### Added

- **Informational rules.** A rule may be `informational`: it runs and reports but never fails the build (`DCA-LAY-005`,
  `DCA-STR-009/010`, `DCA-MAP-011`, `DCA-NAM-002`). `rules.json` carries `status`; `RULES.md` and the counts separate
  enforced (108) from informational (5).
- **Retired rules.** `rules.json` gets a `retired` registry (id, since, reason, replacement); a selection that names a
  retired id is reported one by one (`DcaRuleSelection.retirementNotices()`), `onlyIds`/`dca.rules.ids` with a retired id
  fail with the replacement, exclusions and severities keep loading. Ids are never reused.
- `DCA-USE-016` — use cases do not invoke use cases (direct, via input port, via helper); explicit caller-side coordinator
  exclusions are the only exception. `DCA-USE-017` — the effective public surface of a use case maps onto its input ports
  (inherited and explicit implementations pass; unrelated methods and public properties fail).
- `DcaLayout.withOperationContainers(...)`: optional organisational segments removed before measuring flat/grouped
  use-case depth (`DCA-USE-014`); supporting subfolders define no operations.
- Domain-metadata rules (`DCA-ONI-003`, `DCA-ADV-004/011/015/018`) classify the configured **roles** on types and members
  (fields, methods, constructors; meta-annotations included) and allow unclassified metadata; ownership is exclusive —
  a violation is reported by one rule only.
- `DCA-USE-009` proves the event-free exemption: a `Repository<T,ID>` whose aggregate hierarchy is fully under scan and
  registers no event anywhere (helpers and same-package classes included) needs no publication; unresolved generics,
  partial scans and undecidable helpers keep the requirement.
### Added (framework-neutral vocabulary, WP-30/31)

- `FrameworkAnnotations` presets `jakarta()`, `quarkus()`, `micronaut()` and `none()` next to `spring()`, each a set of
  *roles* (`injectable`, `webController`, `restController`, `transactional`, `eventListener`, `moduleDeclaration`,
  `publishedInterface`, `persistenceEntity`) holding zero or more fully qualified annotation names. `with*(String...)`
  adjusts one role, `named(String)` renames an adjusted set, `describe(role, whenEmpty)` renders a role for messages.
  A rule that forbids a role treats every listed annotation as forbidden, a rule that requires one accepts any of
  them; an empty role selects nothing and passes (`DCA-NAM-002`, `DCA-NAM-005/006`, `DCA-LAY-004`, `DCA-USE-013`,
  `DCA-MAP-006`) or has nothing to forbid (`DCA-ONI-003`, `DCA-ADV-004/011/015/018`); `DCA-USE-012` then counts only
  the explicit `TransactionBoundary`. Fixtures per preset (`fixtures/frameworks/*`) run the full catalog green.
  `spring()` lists JTA's `jakarta.transaction.Transactional` next to Spring's own in the transactional role — Spring
  honours both, so a use case annotated with either satisfies `DCA-USE-012` and is governed by `DCA-LAY-004`/`USE-013`.
- `DcaArchitectureTest` reports the preset in use and how it was chosen as a first, always-passing test
  (`layout / framework annotations: spring (detected)`, `quarkus (detected; also jakarta)`, `spring (default)`,
  `acme (configured)`, `jakarta (explicit)`), so a Jakarta or hand-wired project sees which vocabulary the rules
  resolved and a wrong default is visible instead of silently selecting nothing.
- **Preset SPI and detection (WP-31).** `dev.domaincentric.dca.archunit.spi.FrameworkAnnotationsProvider`
  (`name()`, `annotations()`, `detect(ClassLoader)`, `priority()`), discovered with `ServiceLoader`; the five
  built-ins are providers (`BuiltInFrameworkAnnotations`), a third-party library adds one class and one
  `META-INF/services` line. `FrameworkAnnotations.detect()` asks every provider whether its framework is on the test
  class path (a `getResource` probe on one class file, nothing is loaded) and takes the highest priority — Quarkus
  outranks the Jakarta preset it builds on; two frameworks of *equal* priority (Quarkus and Micronaut on one class
  path) decide nothing: the result is `spring (default; undecided: micronaut, quarkus)` and the project names its
  preset; `FrameworkAnnotations.preset(name)` looks a preset up by name;
  `DcaLayout.withFrameworkPreset(name)` applies it and fails on an unknown name; `dca.framework=<name>` in
  `dca-archunit.properties` does the same for `DcaArchitectureTest` unless the layout set its annotations
  explicitly in code. `DcaLayout.frameworkAnnotationsOrigin()` / `frameworkAnnotationsReport()` expose the choice.
- **`DcaLayout.forBasePackage` now detects.** The default preset is the detected one, Spring when nothing is found
  (`spring (default)`). A Spring project sees no change; a Quarkus or Micronaut project no longer has to name its
  preset.
- `FrameworkNeutralityTest`: fails the build when a rule's title, rationale, `selects` or `checks` — or a
  building-block javadoc sentence — names a framework (`Spring`, `Modulith`, `JPA`, `Jakarta`, `@Service`,
  `@Transactional`, …) or shop vocabulary (`cart`, `checkout`, `product`, `Order`, `inventory`, `pricing`, `customer`)
  outside a sentence marked as an example ("for example", "e.g.", "such as").

### Changed

- `DCA-USE-012` anchors on `Repository.save` and `Repository.deleteById` as well as on the `DomainEventPublisher`;
  violations name the effect (`saves an aggregate`, `deletes an aggregate`, `publishes domain events`). Same per-entry-path
  analysis as before.
- Immutable-shape rules (`DCA-USE-004/005/007`, `DCA-ADV-001`, `DCA-STR-008`, `DCA-TAC-010`) check shallow immutable state on
  classes and records, inherited fields and setters included; the setter heuristic requires `set` followed by an upper-case
  letter (`settle(x)` is no setter); an enum implementing `DomainEvent` is final by construction.
- `DCA-USE-015` rejects same-type and marker-interface aggregate references in results and walks wrappers and part records.
- Controllers (`DCA-HEX-003`, `DCA-NAM-005/006`) are selected by the configured roles or the configured suffixes.
- `DCA-STR-007`: integration-event contracts reside in the configured events segment; translators in
  `adapter/outgoing/event`. `DCA-ADV-006/007` distinguish a schema version from a business revision.
- Entity construction (`DCA-TAC-005`) checks caller roles and context instead of demanding hidden constructors; the
  aggregate-ownership limits are documented in the rule text.
- `DCA-HEX-005`, the domain-metadata rules and the invocation rules report through the shared violation collector, so a
  configured ignore pattern filters single violations instead of dropping the first line.

- **Rule texts speak roles, not Spring.** `DCA-ONI-003` "Domain Models must not carry container or persistence
  annotations" (reads the `persistenceEntity` role instead of hard-coding JPA), `DCA-ADV-004/011/015/018` "… must not
  carry container annotations", `DCA-NAM-002` "Use case classes must carry the injectable stereotype the container
  needs", `DCA-NAM-005/006` select by the configured web-/REST-controller stereotypes, `DCA-USE-012/013` and
  `DCA-LAY-004` speak of "the configured transactional annotation(s)", `DCA-MAP-006` "Upstream declarations and the
  module declaration's allowed dependencies must agree" (Spring Modulith named as the example of a module system).
  Ids unchanged; `rules.json` / `RULES.md` regenerated. Violation messages render the configured annotation
  (`without @Atomic`), not Spring's.
- `ContextMapRenderer` reads the published-interface annotation from the `publishedInterface` role instead of a
  Spring Modulith constant; every configured and loadable annotation counts, a channel is published when the package
  carries any of them under the channel's name; without a configured and loadable one, class presence stands alone
  (as before). `DCA-MAP-006` likewise reads `allowedDependencies` from every configured module declaration a package
  actually carries, not only from the first loadable one.
- The deprecated `FrameworkAnnotations.of(...)` keeps both roles 0.3 hard-coded — JPA `@Entity`/`@Table` and Spring
  Modulith `@NamedInterface` — so a context map rendered through the old factory does not change.
- `DcaLayout.toString()` names the preset (`DcaLayout[com.acme, frameworkAnnotations=spring]`).
- **Custom-rule authors:** `FrameworkAnnotations.restController()`, `transactional()` and `eventListener()` now
  return `List<String>` (the role) instead of a single `String`. `service()`, `component()`, `controller()`,
  `applicationModule()`, `hasApplicationModule()` and the seven-argument `of(...)` remain as deprecated delegates
  onto the roles and go with 1.0.

### Retired

- `DCA-TAC-022` (2026-09-09; covered by `DCA-TAC-014`), `DCA-MAP-003` (renderer disambiguation instead of a forced domain
  rename), `DCA-ADV-003` (duplicate of `DCA-ADV-001` once immutable shape is checked). Listed in `rules.json` `retired` and
  in the catalog's retired registry.

### Fixed

- `DcaArchitecture.contextName` javadoc no longer explains itself through Spring Modulith internals.

## [0.3.0] - 2026-09-08

Depends on `dca-building-blocks` 0.1.2.

### Added

- Every rule describes its mechanics: `DcaRule.selects()` names the classes the rule looks at, `DcaRule.checks()`
  what it asserts about them - including what does not count (`publish(event)` is no publication for
  `DCA-USE-009`; a `Result` that implements `Value` is exempt from `DCA-USE-006`) and what is deliberately not
  established. `rules.json` carries both as `selects`/`checks`, `RULES.md` shows them as two columns; the
  knowledge catalog renders them as **Selection**/**Check** and embeds the private helpers a rule calls, so
  nobody needs the sources jar to predict a rule.

- `DcaLayout.withControllerSuffix(String)` (default `Controller`): the suffix of MVC controllers is configurable like
  the REST one. `DCA-NAM-005` checks it for classes carrying the configured `@Controller` annotation, `DCA-HEX-003`
  selects controllers by either suffix. Previously `Controller` was hard-coded in both rules.

### Changed

- `DCA-USE-009`'s rationale states what its check always did: only `publishAndClearEvents` counts as a publication;
  `publish(event)` per event, even followed by `clearDomainEvents()`, is reported. A fixture pins it.
- `DCA-ADV-012` / `DCA-ADV-016` (stateless domain services and factories) check **inherited** fields too, not
  only the ones the class declares - a mutable field from a base class is state all the same. Aligns with
  the .NET twin.
- `DCA-NAM-010` forbids a sixth suffix in the domain: `Implementation`, the spelled-out `Impl`. Aligns with
  the .NET twin.
- **Rule authors:** `DcaRule.of(...)` and `DcaRule.check(...)` return `DcaRule.Undescribed`; the rule is
  completed with `.selecting(String).checking(String)`, both mandatory (blank text throws). Consumers that
  only run the catalog (`DcaArchitectureTest`, `DcaRules.checkAll`) are unaffected.

## [0.2.0] - 2026-09-07

**Migrating from 0.1.0.** Four things can break a consumer; everything else is stricter enforcement
of the same rules and new rules that a 0.1.0 code base may fail.

1. `dca.rule.<id>.ignore` holds **one** regular expression. Several expressions move to indexed keys
   (`.ignore.1`, `.ignore.2`, …) or are joined with `|`.
2. A context is identified by its package **relative to the base package** (`sales.order`, not
   `order`). `@Upstream(context = …)`, `@Partnership(context = …)` and ignore patterns naming a nested
   context change accordingly; contexts that are direct children of the base package are unaffected.
3. Contexts are discovered by `@BoundedContext` at any depth and the rules select over discovered
   modules — a context that the one-segment wildcard used to skip is governed now.
4. Four new rules (`DCA-USE-014`, `DCA-USE-015`, `DCA-HEX-012`, `DCA-CYC-005`) and the tightened
   `DCA-TAC-003/-007/-008/-012`, `DCA-USE-009/-012/-013`, `DCA-LAY-003`, `DCA-HEX-004/-005`,
   `DCA-MAP-001` and `DCA-STR-003/-004/-006` can report code that passed 0.1.0. Lower a rule to
   `warning(...)` while you fix it — `dca-archunit.properties` documents the reason.

Before 1.0 a minor version may tighten rules; each such change is listed under *Changed — breaking*.
Depends on `dca-building-blocks` 0.1.1 (javadoc corrections only; 0.1.0 works as well).

### Added
- **Shaping the result — two rules.** A use-case result carries values, never identities, and the
  incoming adapter formats what the result delivers instead of operating the domain itself; the
  outgoing side is deliberately different, because repositories and persistence mappers must
  construct and reconstitute domain objects while implementing output ports.
  - `DCA-USE-015` — use case result models must not expose aggregate roots or entities. Selects the
    top-level `*Result` classes below an application package and walks their fields transitively:
    the raw type and every generic type argument of each field (`List<T>`, `Optional<T>`,
    `Map<K,V>`), records nested in the result, and part records anywhere in the application layer —
    next to the result or shared in `application.shared` (`OrderLine`, `CartItemSummary` — named by
    content, without the `Result` suffix). Anything
    assignable to `AggregateRoot` or `Entity` on such a path is reported with the path
    (`ListOrdersResult.highlight -> Highlight.order : Order (AggregateRoot)`); value objects, `Value`
    read models and enriched models may cross. All offending paths of the module land in one
    violation.
  - `DCA-HEX-012` — incoming adapters must not depend on domain services. Selects every incoming
    adapter — event consumers included, they translate and call an input port like any other
    driving adapter — and forbids a dependency on a class assignable to `DomainService`, injected or
    accessed statically. The mechanically exact subset of the doctrine; construction of domain
    objects and calls into domain behaviour from an adapter remain review checks, because a
    blanket type-dependency rule would also reject passive access to values a result delivers.
    Outgoing adapters are outside the selection.
  Catalog: 114 rules.

- **Features within a bounded context — two rules and a compatibility fixture.** A *feature* is an
  optional, domain-named group of related use cases below a module's application package
  (`application.<feature>.<usecase>`, e.g. `checkout.application.session.startcheckout`). It is a
  navigation and cohesion boundary inside one bounded context — not a layer, module, aggregate owner
  or deployment unit, and nothing in the library infers bounded contexts or aggregate ownership
  from it. The pre-existing rules already saw such packages (they select with `..`); the fixture
  `fixtures.features.compat` runs the whole catalog against a grouped context so a later change of a
  selector into a direct-child assumption fails there first.
  - `DCA-USE-014` — use case packages within a module must use one consistent depth: flat
    (`application.<usecase>`) or grouped (`application.<feature>.<usecase>`). Selects the concrete
    classes ending in the configured `useCaseSuffix`, ignores `application.shared`, abstract classes and
    nested types, and reports every offending module and package in one violation — a use case directly in
    the application package, one nested deeper than a feature, or a module that mixes both forms. A
    module without use cases is valid; a single use case may use either depth. Legibility only.
  - `DCA-CYC-005` — the immediate child packages of a module's application package (`shared`
    excepted) must be free of cycles. In a grouped layout those are the features, in a flat layout
    the use cases; a one-directional dependency between two of them is fine. Slices are assigned from
    each class's module root (`moduleRootOf`), never from a one-segment base-package wildcard.
  Catalog: 112 rules.



- `DcaArchitecture.infrastructurePackages()`, `allInfrastructurePatterns()`, `packagesBelowBase()` and the
  static `inPackageTree(packageName, root)`; `DcaLayout.infrastructurePackage(modulePackage)` and
  `channelSubpackage(Upstream.Consumes)`; `ContextMapRenderer.externalSystemNodeId(String)`.
- `DcaLayout` validates what it is given: a base package must be a package name, a sub-package setting
  exactly one segment (`withDomainSubpackage("domain.model")` is rejected), a suffix part of a class name.
  Internally the thirteen positional copy calls are gone — one settings carrier, one constructor that
  validates. The public fluent API is unchanged.
- The catalog's ids and set names are built once (`DcaRules.allIds()` / `setNames()` are now unmodifiable
  views); `DcaRuleSelection` validated every setting against a freshly built ten-set catalog before.
- **Tooling.** CI runs the tests on Java 17 as well (`-PjavaToolchain=17`, the oldest supported runtime —
  the build toolchain stays 21), runs the minimal consumer against the checkout (`-PwithDcaJava`) *and*
  against the local publication (`-PfromMavenLocal` with the versions from `gradle.properties`, which is
  what tests the generated POM), and asserts `META-INF/LICENSE` in both jars. The `Dockerfile` copies the
  `LICENSE` the jar task needs — the image's jars used to lack it — and fails when a jar has none; image
  names are fully qualified (`docker.io/library/…`) so Podman needs no registry alias.
  `scripts/publish-snapshot.sh` validates the effective version of every artifact it is asked to publish
  (with `-P` overrides forwarded to Gradle) before loading credentials; `scripts/lib/artifacts.sh` holds
  the artifact → project → version-property mapping both publishing scripts use.
- **Cycle rules slice by module, not by a one-segment pattern.** `DCA-CYC-001`…`004` used
  `slices().matching(base + ".(*)." + layer + "..")`; a slice pattern needs a capture group to derive
  the slice identity, and `(*)` is exactly one segment — so two contexts grouped below an
  intermediate package produced **no slices at all** and a cycle between them went unreported. They
  now assign slices explicitly from `moduleRootOf(...)`, which holds at any depth. Same detection for
  a flat layout, and pinned by a fixture with a cross-context cycle two segments below the base
  package.
- **`DcaArchitecture.moduleRoots()` — structural module discovery.** A module root is the shortest
  package prefix whose remainder starts with a layer segment (`domain`, `application`, `adapter`),
  so it is found at any depth and without an annotation. Deliberately distinct from
  `boundedContexts()`: being a bounded context is a strategic declaration, owning a layer is a
  structural fact, and the layer rules apply to both — a module that is intentionally not a bounded
  context (an operational backoffice, say) stays governed. Shortest-prefix matching keeps an
  adapter's own `domain` package inside its module.
- **Isolation is structural.** `DCA-STR-003`, `DCA-STR-004`, `DCA-STR-006` and `DCA-HEX-007` used to
  iterate over the *declared* bounded contexts, on the source side and on the target side — so a
  module that owns layers but declares no `@BoundedContext` could import a neighbour's internals, and
  have its own internals imported, without any rule saying a word. They now select over
  `DcaArchitecture.isolatedModuleRoots()` (every module root except the shared kernel), with
  `moduleRootPatternsExcluding()` as the forbidden set and, for `DCA-STR-006`,
  `publishedPackagePatternsExcluding()` — the target's `api` (synchronous) and `events` (asynchronous)
  packages — as the allow-list. Those two package names are DCA's in-process contract convention, a
  convention of the architecture and not of any framework; the rules do not depend on Spring Modulith
  or any module annotation. Declaring a module a bounded context now decides its place on the context
  map, nothing more; a module that is deliberately not a context (one that borrows a foreign
  system's language, say) needs no declaration of any kind and is governed and protected regardless.
  `DCA-STR-006` widens accordingly: everything in a foreign module except `api`/`events` is internal
  — its adapters and infrastructure included, not only its domain and application layers. Titles
  and rationales of the four rules say "module" where they said "bounded context"; `DCA-STR-003` no
  longer promises an "(except allowed dependencies)" list it never had (`DcaRuleSelection`'s
  `ignoringViolationsMatching` is that list).
- **`DcaLayout.withApiSubpackage()` / `withEventsSubpackage()`** — the published-contract packages
  are layout settings like every other segment (defaults `api`, `events`; the two must differ), read
  via `apiSubpackage()`, `eventsSubpackage()`, `publishedSubpackages()`. `DCA-STR-006`, the
  context-map rules (`DCA-MAP-*` channel names, `@NamedInterface` names) and `ContextMapRenderer` all
  take them from the layout; nothing hard-codes `"api"`/`"events"` any more.
- **`DCA-STR-005` no longer prescribes an adapter sub-package.** An Open Host Service is a
  relationship pattern, not a transport: in-process it is the `api` package, over the network an
  incoming adapter (REST, gRPC, MCP). The rule now accepts `api/` or anywhere under
  `adapter/incoming/`; the former `adapter/incoming/openhost/` requirement is gone. Title and
  rationale rewritten; `api` follows `DcaLayout.apiSubpackage()`, as does `events` in `DCA-STR-007`.
- **Per-module rules report every module.** A DCA rule that evaluates one ArchUnit rule per module
  used to throw at the first module that failed, hiding the rest. The four isolation rules now
  collect all violations and throw one `DcaRuleViolation`, so the report is complete and
  `ignoringViolationsMatching` can tolerate individual entries.
- `DcaLayout.sharedOutputPortPattern(String contextPackage)`, the per-context variant that was
  missing.
- `package-info` lookups and context-root tests are memoised per `DcaArchitecture`; discovery now
  walks every ancestor package, and the reflective misses dominate otherwise.

### Changed — breaking
- **An `.ignore` property value is one regular expression.** `dca.rule.<id>.ignore` was split on commas
  like a list of rule ids, so `Foo.{1,3}Bar` failed as an invalid expression (`Foo.{1`) and a comma inside
  a character class changed the meaning of an exception. The value is now taken as written; a second
  expression for the same rule uses an indexed key (`dca.rule.<id>.ignore.1`, `.ignore.2`, …, applied after
  the unindexed one in numeric order). A configuration that listed several expressions in one comma-separated
  value must be split into indexed keys — or into one alternation (`a|b`). The fluent
  `ignoringViolationsMatching(id, regex)` is unchanged.
- **Bounded contexts are discovered by annotation, at any depth.** `DcaArchitecture.rootContextPackage`
  now walks up from a class's package to the nearest ancestor whose `package-info` carries
  `@BoundedContext` or `@SharedKernel`, instead of cutting at the first segment below the base
  package. A context may therefore be grouped (`base.sales.order`), and a single-context application
  may annotate its base package. Contexts that are direct children of the base package resolve
  exactly as before.
- **A context's identifier is its name relative to the base package.** `DcaArchitecture.contextName`
  replaces the last-segment `simpleContextName` for `@Upstream(context = ...)` resolution, the
  rendered context map and rule messages: `base.sales.order` is `sales.order`, not `order`. This is
  how Spring Modulith derives an application-module identifier, so the two agree without a mapping
  layer — and unlike the last segment it cannot collide between two groups. For a context that is a
  direct child of the base package the identifier is unchanged. `simpleContextName` is deprecated.
- **Rules select over discovered modules, not over a wildcard.** The rules no longer use
  `DcaLayout.domainPattern()` and its no-argument siblings (`base.*.domain..`, which matched exactly
  one segment). They select through the new `DcaArchitecture.allDomainPatterns()`,
  `allApplicationPatterns()`, `allAdapterPatterns()`, `allDomainModelPatterns()`,
  `allIncomingAdapterPatterns()`, `allOutgoingAdapterPatterns()` and `allSharedOutputPortPatterns()`,
  built from `moduleRoots()`. The wildcard accessors on `DcaLayout` remain for tooling that needs a
  pattern without an imported class graph.
- The previous `allIncomingAdapterPatterns()` / `allOutgoingAdapterPatterns()` (contexts plus shared
  kernel) now cover every module root, which is a superset.

### Changed
- **A context with no domain layer is no longer a failure.** `DCA-LAY-002`, `DCA-ONI-001`,
  `DCA-ONI-002`, `DCA-ONI-003`, `DCA-HEX-001` and `DCA-CYC-001` select the domain layer and now carry
  `allowEmptyShould(true)`, as the use-case rules already did. A bounded context is a boundary of
  language; which tactical patterns live inside it is a decision per subdomain, and a supporting or
  generic subdomain may legitimately be a transaction script — a use case over a `Store`, no
  aggregate, no `domain/` package. Previously such a project failed those six with ArchUnit's "failed
  to check any classes": an empty subject reported as a defect, with a message naming the rule
  instead of the situation. It only surfaced when *no* module had a domain layer, which is why a
  mixed code base never saw it.

  This does not restore the silence the rest of this release removes. A module whose layers exist is
  found structurally by `moduleRoots()`, so its rules — layer and isolation alike — have subjects.
  Only the genuinely empty case is quiet.

### Fixed
- **Ignoring one violation no longer hides another.** The context-map rules (`DCA-MAP-001` … `-012`),
  `DCA-STR-002` and `DCA-LAY-004` used to throw at the first finding — a `require(...)` or an ArchUnit
  `check(...)` inside a loop — so a rule with two violations and an `ignoringViolationsMatching` pattern
  for the first one reported **PASSED** while the second stood. All of them now collect every violation
  first (the `CollectedViolations` accumulator the four isolation rules already used, extended with
  `require`, `add` and `addAll(rule, classes, explanation)`) and throw one `DcaRuleViolation`, whose
  entries the selection filters individually. Regression: `fixtures.collect` — two dangling upstreams,
  two undeclared edges, two misplaced transactions, a shared kernel depending on two contexts — with one
  entry tolerated each.
- **Transaction rules check the entry path, not the class.** `DCA-USE-012` accepted `@Transactional` on *any*
  method and `TransactionBoundary.inTransaction(...)` *anywhere* in the class as covering every
  publication; `DCA-USE-013` treated one annotated method as making the whole class transactional;
  `DCA-USE-009` only asked that a `save` and a `publishAndClearEvents` call exist somewhere in the class.
  All three now follow the *directed* calls within the class (`IntraClassCalls`: callers, callees, entry
  points and the units reachable on routes through a given kind of unit). An *entry point* is a unit callable
  from outside the class — any non-private, non-synthetic method or constructor, so a public method stays an
  entry point even when another method of the class also calls it — or a unit nothing in the class calls.
  `DCA-USE-009`: for every method that calls `save`, every entry point reaching it must also reach a
  `publishAndClearEvents` — so an entry method may save through one helper and publish through another,
  over any number of steps, but a helper two entry methods share (validation, logging) does not connect
  them, and a saving helper shared by a publishing and a non-publishing entry method is reported for the
  latter (`Foo.executeQuietly (via persist)`); a public `execute` that only saves is reported even when a
  public `complete` calls it and publishes afterwards — the direct call is a path of its own. `DCA-USE-012`:
  for every publishing method and every entry point reaching it, *no route* from the entry down to the
  publication may be free of an annotation or a boundary — an annotated caller does not cover another,
  unannotated path to the same helper, and a boundary on one route (`execute -> wrapped -> publish`) does not
  cover a second route (`execute -> plain -> publish`) to the same publisher. `DCA-USE-013` keeps the wider
  reading (a remote call inside *any* transactional path is a finding). Recursion and mutual recursion
  terminate; a method reached only from within a cycle is its own entry point. Rationales of `DCA-USE-009`
  and `DCA-USE-012` state the remaining limit: ArchUnit folds a lambda's body into the enclosing method
  (the synthetic `lambda$…` methods are not code units of the imported class), so whether a publication
  sits *inside* the block handed to `inTransaction(...)` — and in which order two calls run — is not
  visible in its call model and stays a review check. Fixtures: `fixtures.transactions`.
- **Identities hidden in containers and behind generic base classes are found.** `DCA-TAC-003`, `-007` and
  `-008` only looked at the first type argument of a `List`, `Set` or `Collection` field, so a `Value`
  holding `Map<String, Order>`, `Optional<Shipment>`, `Order[]` or `List<List<Customer>>` passed. The three
  rules now walk every type a field involves (`TypeInspection.involvedTypes` — erasure, array component,
  type arguments and wildcard bounds, recursively, on ArchUnit's type model), shared with `DCA-TAC-017` and
  `DCA-USE-015`; the message says `of type X` for the field's own type and `containing X` for a hidden one.
  An inherited field is read in the context of the inspected class: for `class Base<T> { T value; }` and
  `class Shipment extends Base<Order>` the field involves `Order`, through any number of levels
  (`Intermediate<U> extends Base<List<U>>`) and inside containers; an unbound type parameter contributes its
  bounds, and a type parameter no field uses exposes nothing. **`DCA-TAC-003` keeps its one tolerance
  exactly:** a direct field of the aggregate's own type (a parent, a root, a predecessor) is a
  self-reference and passes; a container of the own type (`List<Order>` in `Order`, now also
  `Optional<Order>`, `Order[]`, `Map<String, Order>`, `List<List<Order>>`) holds *other* instances of the
  aggregate and is reported, as `List<Order>` already was.
- **`DCA-USE-015` includes inherited fields and reports every path.** A `final AnotherResult extends
  BaseListing` inherited an aggregate field from a base class without the `Result` suffix and passed; the
  walker read declared fields only. It now walks all instance fields (`getAllFields()`, statics excluded,
  sorted by name for a stable report) and replaces the global visited set — which reported only the first
  path through a part record, in hash order — by the records on the current path, so
  `ListOrdersResult.firstLine -> OrderLine.item` and `ListOrdersResult.lastLine -> OrderLine.item` are both
  reported. A field inherited from a generic base class is read as the result binds the type parameter:
  `GenericOrderResult extends GenericBase<Order>` with `T value` in the base reports
  `GenericOrderResult.value : Order (AggregateRoot)`; `BatchedOrdersResult extends Intermediate<Order>` with
  `Intermediate<U> extends GenericBase<List<U>>` resolves to `List<Order>`; `String`, an id or a value object
  bound to the parameter passes, and so does a base whose type parameter no field uses.
- **`DCA-TAC-012` matches the exact signatures.** `boolean equals(Weight other)` is an overload, not an
  override — the class still compares by identity — but passed the name-and-arity check. The rule now
  requires `boolean equals(Object)` and `int hashCode()` (non-static, `Object`'s own excluded).
- **Infrastructure is selected by exact package, at both levels.** `infrastructureImplementation()` matched
  `base.infrastructure.` as a prefix, so a class directly in `base.infrastructure` escaped `DCA-LAY-003`,
  `DCA-HEX-004` and `DCA-HEX-005`, and a module's own `base.cart.infrastructure` was never considered.
  `DcaArchitecture.infrastructurePackages()` now lists the global package plus every isolated module's
  `infrastructure` package; the predicate tests package-or-descendant with an exact segment boundary
  (`base.infrastructurex` does not count); `DCA-LAY-002` uses the same list. The shared kernel's
  `infrastructure` package is deliberately **not** in it: everyone may depend on the shared kernel, and
  what it keeps there (a project-wide lifecycle annotation, say) is shared support, not one module's
  detail — the reference implementation's adapters carry such an annotation.
- **`DCA-NAM-011` works for grouped and single-context layouts.** The rule still built
  `base.*.adapter.incoming.web..`, so `base.sales.order.adapter.incoming.web.OrderViewModel` and a
  view model of an application whose base package is the context failed. The allowed packages are now
  derived from the module roots, like every other selector.
- **`DCA-MAP-001` sees declarations on nested packages.** A `@Partnership` on
  `cart.application.getcart/package-info.java` was neither reported nor rendered, because the rule
  inspected the resolved context roots only. It now inspects every package below the base package that an
  imported class lives in, ancestors included (`DcaArchitecture.packagesBelowBase()`), and requires the
  declaring package itself to carry `@BoundedContext`.
- **`DCA-MAP-012` no longer stops at a dangling partner.** Collecting instead of throwing exposed that
  the symmetry check would have dereferenced a missing partner; it now records the dangling declaration
  and moves on.
- **External-system node ids are locale-independent.** The renderer lower-cased with the default locale
  while `DCA-MAP-003` used `Locale.ROOT`; under a Turkish locale `Carrier API` rendered as
  `ext_carrier_ap_` and the two disagreed. One public function,
  `ContextMapRenderer.externalSystemNodeId(String)`, serves both. Annotation text is now escaped for its
  context: `|` and line breaks in markdown cells, `"` (`#quot;`) in Mermaid labels. Channel names come
  from `DcaLayout.channelSubpackage(Consumes)` in both the rules and the renderer.
- **Empty selections no longer fail a greenfield project.** `DCA-HEX-002`, `DCA-HEX-004`,
  `DCA-HEX-005`, `DCA-HEX-006` and `DCA-LAY-003` now pass when nothing matches their `that()`
  clause — a context that has a model and a use case but no adapter or infrastructure yet is a
  legitimate first day, not a violation. `GreenfieldTest` runs the whole catalog against such a
  context (`fixtures.layout.greenfield`) so no rule regresses into ArchUnit's fail-on-empty default.


- **A context nested one package level too deep is no longer ungoverned.** Under the wildcard
  patterns, `base.contexts.todo.domain..` matched no layer rule: every `noClasses()` rule over it was
  vacuously true (or failed with ArchUnit's "failed to check any classes", depending on
  `archRule.failOnEmptyShould`), so the suite reported success while enforcing nothing. Structural
  module discovery governs such a layout — layers and isolation alike, declaration or not.

  (During development of this release a rule `DCA-LAY-006`, "modules must declare whether they are a
  bounded context, the shared kernel, or neither", was tried and dropped again before release in
  favour of structural isolation: demanding a declaration does not remove a forbidden import, and its
  third acceptance depended on Spring Modulith being on the class path. The id was never released and
  is free.)

## [0.1.0] - 2026-08-31

### Added
- **Configurable rule selection.** `DcaRuleSelection` decides which rules run and how strictly:
  `onlySets` / `onlyIds` narrow the run, `excluding(id, reason)` switches a rule off,
  `warning(id, reason)` reports it without failing the build,
  `ignoringViolationsMatching(id, regex)` tolerates a documented exception, and `frozen(ids)` plus
  `withFreezeStore(path)` accept today's violations as a baseline (ArchUnit's `FreezingArchRule`).
- **Configuration without Java.** The same settings can live in `dca-archunit.properties` on the test
  class path; the base class reads it and merges `additionalSelection()` on top. Unknown rule ids and
  set names fail the run instead of silently leaving a rule enforced. Configure rules in code by
  overriding `additionalSelection()` — overriding `selection()` replaces the file rather than adding
  to it.
- **Lowered rules stay visible.** A rule at `WARN` or `OFF` is reported as an aborted test carrying
  its recorded reason, rather than disappearing from the run.
- **Report grouped by rule set.** `DcaArchitectureTest` now produces one dynamic container per set
  (`tactical (22)`, `hexagonal (10)`, …) instead of a flat list of 109 tests.
- **New public API:** `DcaSeverity`, `DcaRuleSelection`, `DcaRuleExecution`, `DcaRuleOutcome`,
  `DcaRuleViolation`, `DcaRule.archRule(...)`, `DcaRules.select/selectFlat/allIds/setNames`,
  `DcaRules.checkAll(architecture, selection)` and the `additionalSelection()` hook.
  `excludedRuleIds()` and `rules()` keep working.

Rule identifiers, titles and rationales are unchanged — `RULES.md` and `rules.json` are identical.

- Initial extraction from the DCA reference implementation.
- `DCA-USE-012` — use cases that publish domain events must be transactional (`@Transactional` on class or method): without an active transaction Spring skips after-commit listeners (`@TransactionalEventListener`, `@ApplicationModuleListener`) silently and Modulith registers no publication. Accepts `TransactionBoundary.inTransaction(...)` as the boundary.
- `DCA-USE-013` — transactional use cases must not call remote-capable output ports (anything but `Repository`, `Store`, `DomainEventPublisher`, `IntegrationEventPublisher`, `TransactionBoundary`): a remote round trip inside the transaction holds the connection and cannot be rolled back. Catalog: 109 rules.
- `DCA-HEX-011` — incoming adapters must depend on input port interfaces, not on use case classes. Injecting the concrete implementation couples the adapter to one realisation, defeats the Dependency Inversion Principle the port exists for, and makes the adapter untestable without the real use case. Catalog: 110 rules.

- 2026-09-09 WP-34 (unreleased): NAM-002 Java diagnostic never fails (.NET n/a); HEX-005 permits own/global infrastructure; ONI-003 and ADV-004/011/015/018 share exclusive role-by-target metadata checks. Java gains injectionSite/persistenceMapping presets and composed detection; .NET gains attribute namespaces and base-attribute detection, replacing the allow-list. No wiring guarantee; no new marker. Shared catalog regeneration pending WP-37.

- 2026-09-09 WP-35 (unreleased): shared new IDs USE-016 (operation invocation, including helpers) and USE-017 (effective public input-port surface); CYC-005 slices operations inside features, respecting containers; MAP-008 requires per-interaction translation evidence without package exclusivity. NET-003 uses the generic interface map (inherited/explicit valid). No coordination marker; anchored caller-side ignore is the explicit exception. Counts await the shared regeneration.

### WP-36 (unreleased 0.4.0)

- `DCA-USE-009` permits event-free saves only with a resolved, fully inspected aggregate; unresolvable types remain checked.
- `DCA-USE-012` has the same id in both languages. Its static graph proves boundary evidence, not block containment.
- **Breaking migration from 0.3.0:** `DCA-STR-007` accepts only the configured events segment. Move contracts from
  adapter/outgoing/event to events, or temporarily exclude DCA-STR-007 by id during migration. Translators stay in adapters.
- `DCA-ADV-006/007` intentionally stop banning business `version`; the three explicit schema-version names are a heuristic.
- `DCA-HEX-006` is directional; `DCA-HEX-007` names integration events and published APIs correctly.


## Catalog kinds and retired identities (2026-09-09)

Catalog entries distinguish enforced rules from informational diagnostics: LAY-001, STR-001, STR-010, MAP-013,
and Java NAM-002. Test runners and generated catalogs report both counts separately. Informational entries do
not prove architectural correctness or runtime wiring. `kind()` / `Kind` is explicit metadata, independent of severity.

Retired ids are never reused: MAP-003 delegates normalized-name collision handling to the context-map renderer;
ADV-003 is covered by ADV-001's immutable-shape check; TAC-022 is covered by TAC-008..012 for value models,
with enrichment guidance in the guide/catalog. `DcaRules.retired()` / `Retired()` retain reason, replacement and
version. Properties exclusions/severity settings and programmatic exclusions using these ids keep loading and
are reported as retired. Unknown ids still fail. The change is intentional in unreleased 0.4.0 for 0.3.0 consumers.

USE-001 retains consumer redeclaration coverage; LAY-005 checks imported consumer implementations in the reserved
building-blocks output-port namespace/package. An imported original interface passes. Name-discovery rules remain:
unmarked types would otherwise evade marker-only selection. Current counts come from generated `rules.json`,
including status and the separate retirement registry, rather than a hard-coded expected total.
