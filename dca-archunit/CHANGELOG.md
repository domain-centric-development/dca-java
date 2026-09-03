# Changelog — dca-archunit

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

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
