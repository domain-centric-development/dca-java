# Changelog — dca-building-blocks

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

### Added

- `application.Refusal` — a marker for a case of a use-case result that refuses the request (WP-171). A use case
  may return its expected refusals as cases of a closed result instead of throwing them, chosen per bounded
  context; every refusing case implements `Refusal`, a success case never does. The marker carries the roll-back
  contract below.
- `application.PortException` (abstract) — the base of every failure a port's contract declares, with two abstract
  branches: `InputPortException` (declared by an input port, raised by the use case, caught by the incoming
  adapter) and `OutputPortException` (declared by an output port, raised by the adapter that implements it, caught
  by the use case — for an expected conflict the store or remote system sees, not for infrastructure failure).
  Neither base is caught as such; each is a classification (WP-171, decision 8).

### Changed

- `UseCaseException` now extends `InputPortException` (was `RuntimeException`). Name, constructors and use are
  unchanged and it stays assignable to `RuntimeException`; a concrete failure keeps extending it. Javadoc: raised
  only by a use case, never by an outgoing adapter; in a context that returns its refusals as cases of a result,
  not-found and not-owned are cases, not exceptions — both styles valid, uniform per context.

- `TransactionBoundary` contract: when the work returns a `Refusal`, the boundary rolls back and returns the
  refusal unchanged, without throwing — a returned refusal undoes earlier writes exactly as a thrown failure does.
  Only the returned value is looked at; a wrapper type that carries a refusal lets its refusing case implement
  `Refusal` too. Nested, a refusal behaves like a failure: the shared transaction becomes rollback-only, an
  outermost block that returns a refusal rolls back cleanly, one that returns a success throws.

`DomainEvent` javadoc: a domain event has no published contract and no version number, but once an
outbox or event publication registry stores it for a transactional listener, the next deployment
reads what the previous one wrote — a change follows what a database migration needs. The former
"can evolve freely without versioning concerns" held only for delivery that completes inside the
transaction.

## [0.3.1] - 2026-09-22

Documentation-only release: no type, signature or behaviour changed, binary-compatible with 0.3.0.
The javadoc flows verbatim into the knowledge catalog and is guarded by `dca-archunit`'s
`FrameworkNeutralityTest`, so a doctrine written into a marker's contract reaches readers only once
the artifact is published.

### Changed — documentation

- `DomainGateway` states that it never writes to the outside. A gateway obtains facts the domain does
  not hold, or delegates a technology-bound computation, so that the model can decide; it does not
  change the state of any external system. Persisting, sending, publishing and calling a remote
  operation that has an effect are the application's business, through an output port. A computation
  that touches nothing outside the process — hashing a password, converting a currency with a
  supplied rate — is a gateway, because no external state changes. The condition list says
  "read-only towards the outside" where it said "side-effect-free or read-only operations are the
  typical case". Recorded as a decision on 2026-09-22.
- `UseCase` calls its output a Result, not a Response. The ubiquitous language of the port hierarchy
  is Command / Query / Result everywhere else, including `DCA-USE-004`'s message.

## [0.3.0] - 2026-09-21

### Added

- `ddd.tactical.DomainException` and `application.UseCaseException` — abstract, unchecked base classes for the two
  kinds of failure an inner layer raises: a broken business rule of the model and a request the use case cannot
  serve. Neither carries a code or status field; mapping an outcome onto a protocol answer is the incoming
  adapter's decision. The word is `UseCaseException` in both libraries, because the .NET platform occupies
  `ApplicationException` and discourages deriving from it. The `errors` rule set of `dca-archunit`
  (`DCA-ERR-001` … `DCA-ERR-006`) checks their use; `docs/adr/adr-002-exception-base-types-and-error-rules.md`
  records the decision.

## [0.2.0] - 2026-09-10

**Migration from 0.1.2.** `BaseAggregateRoot.registerEvent` is `protected`: an aggregate registers its own events, in the
method that produces the fact (`Product.create(...)` raises `ProductCreated`); a factory or use case that used to call
`aggregate.registerEvent(...)` from outside no longer compiles — move the call into the aggregate's creation or
behaviour method and let the factory delegate. Everything else in this release is documentation.

### Changed — breaking

- `BaseAggregateRoot.registerEvent` is `protected` (was public). The aggregate owns its event registration; nothing
  outside its hierarchy can add an event to it. Same visibility as the .NET twin. `dca-archunit` 0.4.0 relies on this:
  `DCA-USE-009`'s event-free exemption can prove "never registers an event" only because no foreign helper can reach
  the method.

### Changed — documentation (framework- and shop-neutral javadoc)

The javadoc flows verbatim into the knowledge catalog and is guarded by `dca-archunit`'s `FrameworkNeutralityTest`.

- `DomainEvent`, `DomainService`, `Factory`, `Specification`, `Repository`: "Should NOT have Spring annotations"
  became "carries no container stereotype / no framework annotations" — the rule is about the container, whichever
  it is.
- `TransactionBoundary` explains the concept first and names Spring's `TransactionTemplate` and JTA's
  `UserTransaction` as two implementations; the default boundary is "the framework's transactional annotation",
  nesting is "the required propagation every transaction manager offers".
- `Upstream.context()` / `Partnership.context()`: "module name (Spring Modulith module name)" became "the context's
  package name relative to the base package" — which is what the rules derive; `ExternalUpstream` says "context
  name".
- Examples in prose use neutral domains (`ContractId`, `TariffCalculator`, `ShipmentDispatched`); code snippets keep
  their illustrative shop examples. `DomainEventPublisher`: "Sequence of operations".

## [0.1.2] - 2026-09-08

Documentation-only release: no type, signature or behaviour changed; the javadoc that ships in the
`-javadoc` and `-sources` jars is what the knowledge catalog now renders. Binary-compatible with 0.1.1.

### Changed

- `Repository`'s javadoc names `findById`/`save`/`deleteById` as the deliberate minimum and says that a
  concrete port extends them freely (`existsBySku`, `count`, `deleteAll`) - the marker adds no generic
  `existsById`/`findAll` on purpose. Documentation only; the sentence flows verbatim into the knowledge
  catalog's marker node.
- `Id`, `Entity` and `Value` had no javadoc at all - their knowledge-catalog nodes read "Marker for
  Entity." They now describe the contract: identity vs. attribute equality, why identifiers are typed,
  non-root Entities created only through their root and never returned from a Repository, Value Objects
  immutable and self-validating; `Entity.id()` and `sameIdentityAs` are documented.
- `AggregateRoot`'s usage pattern showed `domainEvents().forEach(eventPublisher::publish)` followed by
  `clearDomainEvents()` - the form `DCA-USE-009` rejects. It now shows
  `eventPublisher.publishAndClearEvents(product)` and says that this is the one sanctioned form.

## [0.1.1] - 2026-09-07

Documentation-only release: no type, signature or behaviour changed; the javadoc that ships in the
`-javadoc` and `-sources` jars is corrected. Binary-compatible with 0.1.0.


### Fixed

- `UseCase`'s javadoc taught `CreateProductCommandPort` and a `{Action}{Entity}UseCase` naming pattern for
  the port — contradicting `DCA-NAM-003`, which requires `*InputPort`, and `DCA-NAM-001`, which reserves
  `*UseCase` for the implementation. The example now reads `CreateProductInputPort` /
  `CreateProductUseCase`, and the interface no longer redeclares `execute`.
- `Specification`'s example implemented the raw interface with `isSatisfiedBy(Product)`, which does not
  override the generic `isSatisfiedBy(T)`. The example is generic (`Specification<Product>`) and its
  combinator returns a lambda. (The knowledge catalog carries each marker's first javadoc sentence and
  signature; the examples live in the javadoc and sources jars.)

### Added

- A contract test suite (test scope only — the artifact stays dependency-free) for the executable parts:
  event registration, read-only exposure and clearing on `BaseAggregateRoot`, identity comparison via
  `Entity.sameIdentityAs`, and `TransactionBoundary`'s `Runnable` overload delegating to the `Supplier`
  boundary.

- `OpenHostService`'s javadoc prescribed `adapter/incoming/openhost/` and called the service an
  incoming adapter — while the reference implementation places every Open Host Service in the
  context's `api/` package and the rule accepted both. Rewritten: an Open Host Service is the protocol
  a context publishes, a relationship pattern independent of transport; in-process it is `api/`,
  over the network any incoming adapter. The javadoc flows verbatim into the knowledge catalog.

- `Repository`'s javadoc contradicted itself: "Repository interfaces belong in the domain layer"
  under *Key Principles*, while *Characteristics* nine lines later placed the interface in the
  application layer as an output port. The application layer is correct — the first line is removed
  and the example's comment corrected. The text is copied verbatim into the knowledge catalog's
  marker nodes, so the wrong sentence had travelled.

## [0.1.0] - 2026-08-31

### Added
- Initial extraction from the DCA reference implementation.
- `application.TransactionBoundary` — explicit transaction boundary inside a use case; an application-layer execution abstraction implemented by infrastructure, deliberately not an output port (remote calls stay outside the transaction).
