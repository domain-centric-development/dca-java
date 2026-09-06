# Changelog — dca-archunit

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

### Fixed

- **Empty selections no longer fail a greenfield project.** `DCA-HEX-002`, `DCA-HEX-004`,
  `DCA-HEX-005`, `DCA-HEX-006` and `DCA-LAY-003` now pass when nothing matches their `that()`
  clause — a context that has a model and a use case but no adapter or infrastructure yet is a
  legitimate first day, not a violation. `GreenfieldTest` runs the whole catalog against such a
  context (`fixtures.layout.greenfield`) so no rule regresses into ArchUnit's fail-on-empty default.

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


### Changed — breaking

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

### Added

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
