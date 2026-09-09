# AGENTS.md

Guidance for AI coding agents working in `dca-java` — the Java libraries of Domain-Centric Architecture:
`dca-building-blocks` (markers, ports, `TransactionBoundary`), `dca-archunit` (the rule catalog), `dca-spring`
(Spring implementations of the runtime ports) and `dca-archunit-spring-modulith` (Spring Modulith verification).
`README.md` explains the artifacts, `RELEASING.md` the release procedure, `docs/adr/` the decisions.

## Principles that apply in every DCA repository

These hold for anyone working in any of the DCA repositories — human or agent — regardless of local tooling or memory.

1. **The samples exist to make the AI harness deterministic, not to ship features.** They are the experiment field
   for the harness: knowledge catalog, architecture rules, markers, plugins. Every architectural change in a sample
   answers three questions before it is done — does the catalog need a node (pitfall / decision / recipe /
   template)? could an ArchUnit / ArchUnitNET rule check it (same rule id in `dca-java` and `dca-dotnet`)? would one
   more *generic* marker in the building blocks make it checkable? Record the answer, "none" included, in the WP or
   ADR. Two agents building the same thing differently in the two samples is a determinism gap to close at the
   source (catalog, rule, marker), not with a code fix.
2. **Rules, markers and the catalog are general.** They are the foundation other production systems — any industry —
   build on with AI. Nothing in them may exist only because the e-commerce sample needs it: no shop vocabulary in
   rule or marker names or texts, no selection that only matches the sample's layout, no catalog node that presupposes
   a cart. The sample proves the general artifact; it is never its source of names or shapes.
3. **The core libraries are framework-neutral.** `dca-building-blocks` / `DomainCentric.BuildingBlocks` have zero
   dependencies; `dca-archunit` / `DomainCentric.ArchRules` reference frameworks only as configurable presets
   (`FrameworkAnnotations`, `FrameworkTypes`) and never on their class path. Framework-specific code goes into
   satellite artifacts (`dca-spring`, `dca-archunit-spring-modulith`) or into the sample. Spring is the default
   preset and the reference implementation's framework, not the vocabulary of the rules.
4. **Each reader artifact stands alone.** Guide, book and catalog bundle are independently readable; links never
   cross repository boundaries (the sample may cite the guide). `AGENTS.md` files are exempt — they carry the
   cross-project pointers and the sync duties.

## Working here

- **Build:** `./gradlew build` (all four artifacts, self-tests, `verifyFrameworkFree` on the two core artifacts,
  spotless). `./gradlew :dca-archunit:rulesCatalog` regenerates `rules.json` / `RULES.md` — commit them with every
  rule change; the knowledge catalog reads `rules.json`.
- **Rule texts are read by agents.** Every rule carries `title`, rationale, `selects()` and `checks()`; they flow
  verbatim into the catalog. Phrase them for an arbitrary domain and framework ("the configured injectable
  annotation", not "@Service"; "an owned aggregate", not "a cart"). Never cite the sample's ADRs.
- **New rule = new id, same id in `dca-dotnet`.** Port it there or list it as not applicable with a reason
  (`PORTING-LOG.md`, `planning/porting-status.md` in the monorepo). A new rule is a minor bump of `dca-archunit`.
- **Markers are rare.** A new interface in `dca-building-blocks` needs a rule that selects on it and a second,
  unrelated domain that would want it. Javadoc flows into the catalog: no framework or shop vocabulary.
- **Versions** live in `gradle.properties`; `buildingBlocksVersion` and `archunitVersion` name the *released* versions
  the dependent artifacts pin in their POMs. Release with `scripts/release.sh <artifact> <version>`, tag afterwards.
- **Consumers to keep in sync** (monorepo checkout): `dca-ecommerce-sample-java` (uses all four artifacts), the
  bootstrap skill in `dca-marketplace`, `dca-guide/archunit-governance.md`, the knowledge catalog (regenerate).
