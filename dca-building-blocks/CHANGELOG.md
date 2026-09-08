# Changelog — dca-building-blocks

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

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
