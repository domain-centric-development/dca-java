# Changelog — dca-archunit

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

**Every name a rule matches on is configurable.** `withAggregateRootSuffix`,
`withRepositorySuffix`, `withStoreSuffix`, `withFactorySuffix` and `withSpecificationSuffix` join
the use-case and controller suffixes, so `DCA-TAC-001`, `DCA-TAC-013`, `DCA-TAC-016`, `DCA-TAC-018`,
`DCA-ADV-013` and `DCA-ADV-017` follow the project's names instead of the library's. A code base whose ports are called `*Gateway`
configures the layout; it no longer has to switch the ids off. Defaults unchanged.

**`DCA-ADV-009` is retired; `DCA-ADV-010` keeps the layer.** Both selected the same population —
non-interface types assignable to the domain-service marker — and differed only in what they
required of the package: `..domain.service..` anywhere versus the domain layer of a module. A
misplaced domain service was reported twice, and the stricter half asked for something the guide
itself does not ask for (`topics/domain-services-with-data-dependencies.md` allows `..domain..`). A
domain service is part of the model; whether it gets its own segment inside the domain layer is the
team's decision, per project. `DCA-ADV-010` continues to keep it out of the application layer.
`withDomainServiceSubpackage` goes with it — `DCA-ADV-009` was its only reader, and it was never
released.

**`DCA-ADV-013` and `DCA-ADV-017` are titled after what they check.** "Factories should implement
Factory Marker Interface" selected the marked types and checked the *name*; "Specifications must end
with 'Specification'" selected by the name and checked the *placement* — and since the specification
role was added it does not require the suffix at all. They are now "Types carrying the factory role
are named *Factory" and "Specifications reside in the domain layer", with rationales that argue the
check rather than the selection. Open since the review of 2026-09-09.

**`DCA-TAC-015` names its overlap with `DCA-HEX-008`.** An implementation that carries the
repository role and is also named `*Repository` is reported by both; the two populations are
different, and the `checks` text says so now.

**The READMEs have a "smallest DCA" and an "own vocabulary" section.** Seven building blocks and one
`@BoundedContext` are enough for the catalog to say something useful; every other marker switches on
the rules that govern it when the project introduces the concept. `DcaMarkers` appeared in no README
at all, although keeping an existing vocabulary is the library's main adoption argument — it is
documented now, together with the one caveat (`withOutputPort` once any port role is set) and with
the note that the aggregate-root marker's event API is inherited whether or not a project models
events.

**`DCA-TAC-016` reads the aggregate the repository binds, and the name must name it.** The rule
resolved the aggregate from the interface name alone and never looked at the type argument, so
`CategoryRepository extends Repository<Order, OrderId>` passed. It now takes the first type argument
of the parameterised marker, requires it to be an aggregate root, and requires the interface's simple
name to be that type plus `Repository`. A generic intermediate port — `AuditedRepository<T, ID>` —
binds a type variable and is skipped instead of reported as "no class named Audited". Where the
marker is not generic or is used raw, the old name resolution still applies. Decision R3 is withdrawn
and replaced: both, not either.

**`Specification` is a role, not only a suffix.** `DcaMarkers` gains `specification`;
`DCA-ADV-017` (placement) and `DCA-ADV-018` (metadata ownership) select through the marker **or** the
name. Both reference samples name their specifications after the predicate they express —
`HasMinTotal`, `ActiveCart` — and carry the marker through an intermediate interface, so under
name-only selection neither rule saw them and a container stereotype on one was reported by nothing.
`DomainGateway` stays vocabulary; no rule reads it, and the marker says so.

**Every violation can carry a remedy, and eleven more rules do.** See the entry above; with this
release `DCA-TAC-016` names its remedy too.

**Dead public API is gone while the version is unpublished.** Four unused package constants
(`BUILDING_BLOCKS_PACKAGE`, `_TACTICAL_`, `_STRATEGIC_`, `_PORT_`) and the two public factory methods
for the retired `DCA-ADV-003` and `DCA-TAC-022` are removed — the methods produced rule ids that
`DcaRuleSelection` refuses and the report cannot place. `BUILDING_BLOCKS_PORT_IN_PACKAGE` and
`…_PORT_OUT_PACKAGE` stay; they carry `DCA-USE-001` and `DCA-LAY-005`. A new test asserts that no rule
set offers a retired id.

**`DCA-NAM-003` and `DCA-HEX-009` say what they presume.** `DCA-NAM-003` states that the `I` prefix is
a platform convention checked only in .NET. `DCA-HEX-009` and the `DcaMarkers` javadoc state that
`withOutputPort` must be set whenever any other port role is, and that a vocabulary without a common
port root should switch the id off rather than point the role at an unrelated type.

**One more hard-wired segment.** `DCA-USE-014`'s depth check still read a literal `shared`; it reads
the configured segment now, like the rest.

**Every violation can carry a remedy, in both languages.** `DcaRule` gained `remedy()` and
`checking(checks, remedy)`; `DcaRuleExecution` appends it as one `Fix:` line under the violations,
prefixed with the rule id like every other line. Eleven rules name theirs — `DCA-CYC-005`,
`DCA-HEX-012`, `DCA-NAM-001`, `-003`, `-004`, `-005`, `-006`, `DCA-STR-008`, `DCA-USE-009`, `-014`
and `-015` — the same wording the .NET twins have carried in `DcaRule.Fail` since 0.4.0. Unlike
.NET's, the parameter works for rules built with `DcaRule.of(...)` too, whose ArchUnit report says
only what is wrong.

**`DCA-TAC-009` no longer reports an enum value object.** An `enum Currency implements Value` with
constant-specific class bodies is compiled abstract, so the rule demanded a `final` modifier the
language forbids — a false positive with no fix. Enums are no longer selected, as the .NET twin
never selected them. A fixture proves it.

**`DCA-ERR-005` no longer rejects Ubiquitous-Language names.** The word list matched `Status` and
`Response` as substrings, so `OrderStatusInvalidException` and `PaymentResponseMissingException`
failed the rule that exists to protect the language. It now matches the compound transport words —
`Http`, `StatusCode`, `ResponseStatus`, `ResponseEntity` — in both languages. `Http` as a substring
was and stays safe.

**`DCA-TAC-021` matches the same six write names in both languages.** Java knew `save`, `deleteById`
and `delete`; .NET additionally knew their `Async` forms, so a Java store declaring
`CompletableFuture<Void> saveAsync(Entry)` passed where its C# twin failed. Both now match the three
names and their asynchronous forms, compared case-insensitively.

**`DCA-USE-001` matches both spellings of the reserved contract name.** Java caught `InputPort`
only, .NET `InputPort` and `IInputPort`. A Java project that declares `IInputPort` is now caught too.

**`DCA-USE-015` exempts the value role, as .NET always has.** A domain value object whose name ends
in `Result` and which lives in an application package was walked in Java and skipped in .NET.
`DCA-USE-006` already lets such a type cross the port; the content walk now agrees.

**Four `DCA-USE` titles say what the rule checks.** `DCA-USE-002`, `-003`, `-006` and `-008` were
titled "must end with 'Command' and reside in application package", but the suffix is the
*selection* — the rule never requires anything to be named that way. They are now "Types named
`*Command` reside in the application layer" and so on. Titles are not ids; nothing breaks.

**The web adapter and shared segments are configurable.** `withWebSubpackage(...)` and
`withSharedSubpackage(...)` join the other segments. `web` was hard-wired in `DCA-NAM-011`, so a
project whose web adapter is `adapter.incoming.ui` had every ViewModel reported with no
configuration exit; `shared` was hard-wired in the shared-output-port patterns and in the cycle
slicing. The defaults are unchanged, and the shared segment stays reserved against the operation
containers whatever it is called.

**`DCA-STR-008` cites the right reason.** Its rationale attributed the immutability of a published
contract to event sourcing, which DCA prescribes nowhere. It now argues from the contract: once
another context has read an integration event, its shape may only grow.

**Four fixture comments described behaviour that no longer exists** — `DCA-TAC-003`'s tolerated
self-reference (it is reported), a retired id, and two use-case-local output ports still marked as
`DCA-TAC-014` / `DCA-TAC-019` negatives, which R3 made legal.

**Six `DCA-ADV` rules and `DCA-STR-007` / `DCA-STR-008` no longer report an intermediate marker
interface.** `DCA-ADV-002`, `-010`, `-012`, `-013`, `-014`, `-016`, `DCA-STR-007` and `DCA-STR-008`
selected every type assignable to their marker role, interfaces included, while both `rules.json`
files said "Non-interface classes" and the .NET twins excluded them. A context that groups several
domain events behind a shared contract interface, or its integration events behind a published-language
interface, was reported in Java and passed in .NET — and `DCA-STR-008` asked that interface to be
"final with final instance fields", which no interface can be. The eight rules now select concrete
types only, as their texts always said. The concrete events are checked exactly as before. This can
turn a red build green, never the other way round.

**`DCA-ERR-002` and `DCA-ERR-003` no longer both report the same exception.** A subtype of
`DomainException` declared in an application package was reported twice, with contradictory remedies:
`DCA-ERR-002` says move it to the domain, `DCA-ERR-003` says give it another base type — which would
turn a broken rule of the model into a use-case failure. `DCA-ERR-002` owns the case now;
`DCA-ERR-003` no longer selects domain-exception subtypes and says so in its `selects` text.

**No logging library in the domain by default, and the allow-list is how you change that.** The two
`DCA-ONI-002` allow-lists were not translations of each other: Java forbade SLF4J, .NET permitted
`Microsoft.Extensions.Logging.Abstractions`. Decided on 2026-09-21: by default neither, in both
languages — which logging, validation or utility library a domain model may see is a project decision,
made with `withThirdPartyPackagesAllowedInDomain(...)`. The Java default is unchanged; the rule text now
says the facade is deliberately absent and names the way to add it.

**The four fixed method names are written down.** `save`, `deleteById`, `publishAndClearEvents` and
`registerEvent` are matched by name and are not marker roles, so a vocabulary whose repository writes
under another name is selected and then found to save nothing — `DCA-USE-009`, `DCA-USE-012` and
`DCA-TAC-021` pass over it. The three `checks` texts and the `DcaMarkers` javadoc now say so instead of
leaving a reader to assume the roles cover it. No new configuration point: roles name types.

**The tree names the version it will ship as, and `rules.json` says which release its texts belong to.**
`archunitVersion` is `0.5.0-SNAPSHOT` and `buildingBlocksVersion` `0.3.0-SNAPSHOT` until the release
pins them, so a locally built jar can no longer be mistaken for the released artifact of the same
number. `rules.json` gained `library` and `version` at the top; the reader that takes `rules["rules"]`
is unaffected. The README now states the public API surface, the versioning promise after 1.0 — a new
or tightened rule is a major bump, not a minor one — and the exact rule counts, taken from the
generated catalog rather than typed in.

**The README states what the library runs on.** A compatibility table now names the minimum and the
tested versions of Java, JUnit Jupiter, ArchUnit, Spring Boot and Spring Modulith. It also states that
`dca-spring` needs Spring Boot 4: its auto-configuration ordering does not exist on Boot 3.x and
nothing fails loudly there.

**New rule `DCA-STR-012` — at least one module owns a DCA layer.** The third guard of the same kind as
an empty import and an undeclared bounded context. A code base whose layers are named `core` and `usecases`
instead of the configured segments yields no module root, so every rule that selects over the layers — the
whole use-case set among them — matched nothing and the suite was green. The violation names the configured
segments, the packages it did see, and the way out. This is the most likely first run of a code base that has
not configured the layout, so **it can turn a green build red**; switch it off with a recorded reason if the
code base deliberately has no layered module.

**`DCA-USE-009`, `DCA-USE-012` and `DCA-USE-013` select what their texts say.** The three selections
were spelled as a chain of `and`/`or`, which ArchUnit joins left to right, so they read
`((inApplication ∧ suffix) ∨ isInputPort) ∧ ¬interface` and reported every input-port
implementation anywhere — a composition-root decorator or a test double in an adapter included.
The .NET twin never did. The selection is now one predicate matching the documented wording:
a non-interface class in an application package that carries the use-case suffix or implements the
input-port role.

**A use case over a generic input port is reported once.** The compiler writes a bridge method
`execute(Object)` beside `execute(Command)`; nothing inside the class calls it, so the "nothing
calls it" fallback made it a second entry point and `DCA-USE-009` and `DCA-USE-012` reported the
same violation twice, the second time as `execute (via execute)`. Synthetic and bridge units are
never entry points now. This is the ordinary shape of a Java use case, so the change halves the
violation count a team sees on its first run.

**`DCA-ONI-002` no longer reports a project's own marker vocabulary.** The rule named the two
building-blocks packages directly, so a project that pointed the roles at its own markers — the
adoption path `DcaMarkers`'s own javadoc shows — was told its aggregate root was a forbidden
third-party dependency inside its own domain, once per aggregate, value object and domain exception,
on the first run. The allowed packages are now derived from the configured roles
(`DcaMarkers.declaringPackagePatternsOf(DcaMarkers.DOMAIN_FACING_ROLES)`). With the default roles
this yields exactly the two packages that were written in by hand, so nothing changes for a project
on the building blocks. The application-layer roles and the incoming ports stay off the list: a
domain class reaching for one of them is what the rule exists to report.

**Every violation now names its rule.** A report line used to carry the offending element and, for six of the
rules, the id; the other rules named it nowhere, so the only thing a reader could match on was the rule title —
which changes as soon as a suffix is configured. `DcaRuleExecution` now prefixes every line of every report with
`[DCA-XXX-nnn]`, in both languages. Nothing about a rule's selection, check or severity changes, and a frozen
baseline stays valid because the prefix is added after the evaluation. A consumer that parses report text sees
the new prefix.

**What can turn a green build red:**

- `DCA-ERR-004` also forbids the new `transportStatus` role — the annotation that fixes the protocol answer
  of the type it sits on (`@ResponseStatus` in the Spring preset, `@Status` in the Micronaut one; empty in
  the Jakarta and Quarkus presets, where the framework answers through a mapper class instead). A domain or
  use-case exception carrying one turns red: which status a failure earns is the incoming adapter's
  decision, and an annotation on the failure decides it for every protocol at once. `withTransportStatus(…)`
  adjusts the role, and the twin setting in `DomainCentric.ArchRules` is `TransportStatusAttributeTypes`.

- **The rules no longer reference the building-block markers as types.** Every selection that used to name
  `AggregateRoot`, `Entity`, `Value`, `Id`, `DomainEvent`, `IntegrationEvent`, `DomainService`, `Factory`,
  `DomainException`, `UseCaseException`, `TransactionBoundary`, `InputPort`, `UseCase`, `OutputPort`,
  `Repository`, `Store`, `DomainEventPublisher` or `IntegrationEventPublisher` now asks the layout for that
  *role*, resolved by fully qualified name through the new `DcaMarkers`. `DcaLayout` defaults to
  `DcaMarkers.dca()`, so nothing changes for a project on the building blocks; a project with its own
  markers or jMolecules points the roles at its own types
  (`withMarkers(DcaMarkers.dca().withAggregateRoot("org.jmolecules.ddd.types.AggregateRoot")…)`) and is
  governed by the whole catalog instead of excluding the affected rule ids. A role must name a type — an
  empty one is refused rather than silently selecting nothing. What the roles do not cover are the
  strategic annotations: the rules read their members, and a name does not carry members.
- Correcting the first cut of this change, found in review: `DomainMetadata` (which assigns `DCA-ADV-004`,
  `DCA-ADV-011`, `DCA-ADV-015` and `DCA-ONI-003` their exclusive owner) still matched the library's own marker
  names, so the two languages would have classified differently under one configuration; the rules that select
  a marker used ArchUnit's `implement(...)`, which matches an implemented interface only, so a vocabulary whose
  marker is an abstract base class selected nothing and reported success — they now select by assignability,
  which is what their `selects` texts always said; and the exclusion of the vocabulary's own code in
  `DCA-ERR-001…005` was hard-wired to this library's package, which reported a project's own exception base
  class as "a domain exception outside the domain layer". The exclusion is now derived from the packages the
  configured role types live in.
- `DcaArchitectureTest` prints the vocabulary as a second diagnostic case — `building block markers: dca
  (library default)`, or the name and the roles that differ. `DcaLayout.markersReport()` is the same line
  without JUnit.
- `DCA-USE-015` and `DCA-TAC-004` name the configured roles in their violations instead of the literal
  `AggregateRoot` / `Entity` / `Id`, so the message stays true under a project's own vocabulary. The
  wording is unchanged for the default roles.

- The `errors` rule set is new, `DCA-ERR-001` … `DCA-ERR-006`, and pins the two new building blocks
  `DomainException` and `UseCaseException`. A project that declares its own exception types in the domain or
  application layer will see `DCA-ERR-001` / `DCA-ERR-003` until those types extend the base type of their layer;
  `DCA-ERR-002` adds the reverse direction (a subtype declared outside its layer), `DCA-ERR-004` forbids the
  configured container, persistence, transaction, controller and event-listener annotations on them, and
  `DCA-ERR-005` forbids the technical suffixes `Error`, `Fault`, `Failure` and the words `Http`, `Status`,
  `Response` in their names. Argument guards are outside all of this: the platform's own argument exceptions are
  never selected. `DCA-ERR-006` is informational — the import model carries neither a `throw` nor a `catch`, so an
  adapter that catches a generic exception can only be listed, never failed. It reports an incoming adapter
  *package*, not a single class: a central exception handler beside the adapters is what the doctrine asks
  for, and a per-class diagnostic would list exactly the adapters that use one.

- `DCA-STR-011` is new: at least one package below the base package declares `@BoundedContext`. Without a
  declaration the context-map and isolation rules select nothing and report success over an empty model. A code base
  that deliberately declares no context switches the rule off with a recorded reason; the structural isolation rules
  keep governing the modules.
- `DcaArchitecture.load(layout)` now imports jars and archives as well. `importPackages` already restricts the
  result to the base package, so what this adds are the sibling modules of a multi-module build - which used to be
  excluded silently, leaving their contexts undiscovered while every rule still reported success. A class path that
  ships the project's own base package in a third-party artifact narrows the import with the new
  `load(layout, ImportOption...)`.
- `DcaArchitecture.load` refuses an import that found no class below the base package, instead of running every rule
  over nothing. `DcaArchitecture.of(layout, classes)` is unchanged - an explicitly imported set is the caller's.
- `DcaRules.only(layout, ...)` rejects an unknown rule-set name and `DcaRules.allExcept(layout, ...)` an identifier
  that is neither in the catalog nor retired; both used to select or exclude nothing silently. `DcaRuleSelection`
  validated these all along.

- `DCA-LAY-004` also sees programmatic boundaries, through two new `FrameworkAnnotations` roles. `transactionApi`
  (the types code runs a transaction with: transaction templates, user transactions) is allowed exactly where the
  annotation is - application layer and outgoing adapters; any other class that depends on one is reported, a
  bootstrap runner in the global infrastructure package included. `transactionManager` (the types a composition root
  declares: platform transaction managers) and `TransactionBoundary` are wiring and plumbing: the global and the
  shared kernel's infrastructure packages may depend on them too; the domain, incoming adapters and a module's own
  infrastructure may not. Implementations of `TransactionBoundary` are exempt everywhere. `withTransactionApi(...)`
  and `withTransactionManager(...)` adjust the roles; `none()` leaves both empty. A bootstrap or web adapter that wraps its work in a transaction template turns red - move the
  boundary into the use case.
- `DCA-HEX-007` title and texts now describe the code: "Incoming adapters depend on no other module, except event
  consumers on the events they subscribe to". An incoming adapter may not depend on any package of another isolated
  module, published `api`/`events` included; the single exemption is the event-consumer sub-package. Behaviour
  unchanged.
- `DCA-MAP-008`, `DCA-MAP-009` and `DCA-MAP-010` separate two questions: placement of code that exists is checked
  for every declaration, PLANNED included; only an IMPLEMENTED `@Upstream` demands that a translation site exists
  (`DCA-MAP-008`, as `DCA-MAP-007` demands an implementation). `DCA-MAP-011` keeps counting a PLANNED declaration as
  declared.
- `DCA-MAP-006` reports a context that declares `@Upstream` edges but carries no configured module declaration once,
  as "module declaration missing on '<context>', allowed dependencies unknown", instead of listing every edge as
  unmatched.
- `DCA-TAC-002` inspects every field, static ones included - a static port breaks persistence ignorance just the
  same; `DCA-TAC-003` stays instance-only (a static same-type field holds no aggregate). Both `checks` texts say so.
- `DCA-NAM-002` no longer lists records, as `DCA-NAM-001` never selected them.
- `DCA-CYC-001..004`: the `checks` texts state the limit - slices are per module root, a cycle inside one module's
  layer is not detected there, `DCA-CYC-005` covers the application layer per operation. No code change.
- Two more layout segments: `withModelSubpackage` (default `model`) and `withIncomingEventSubpackage` (default
  `event`). `DCA-CYC-001` and the domain-model patterns read the first, the event-consumer exemption of `DCA-HEX-006`
  and `DCA-HEX-007` the second; neither is hard-coded any more. `DcaLayout.incomingEventAdapterPattern()` and
  `domainModelPackage(module)` are new.
- `DCA-ONI-002`: the in-code comment describes what the rule allows today - the domain packages of every module root
  plus the building-blocks tactical and output-port packages; no behaviour change.
- `DCA-STR-007`: a test proves that a renamed events segment (`withEventsSubpackage`) is honoured; the rule has read
  the segment from the layout since 0.4.0, nothing in it is hard-coded.

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
