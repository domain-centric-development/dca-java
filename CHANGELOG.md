# Changelog

## Unreleased

### `dca-archunit`

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
