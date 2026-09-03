# Changelog — dca-building-blocks

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

### Fixed

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
