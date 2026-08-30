# Changelog — dca-archunit

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

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
