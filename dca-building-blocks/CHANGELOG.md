# Changelog — dca-building-blocks

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

## [0.1.0] - 2026-08-31

### Added
- Initial extraction from the DCA reference implementation.
- `application.TransactionBoundary` — explicit transaction boundary inside a use case; an application-layer execution abstraction implemented by infrastructure, deliberately not an output port (remote calls stay outside the transaction).
