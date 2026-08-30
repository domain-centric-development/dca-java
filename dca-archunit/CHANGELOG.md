# Changelog — dca-archunit

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

### Added
- Initial extraction from the DCA reference implementation.
- `DCA-USE-012` — use cases that publish domain events must be transactional (`@Transactional` on class or method): without an active transaction Spring skips after-commit listeners (`@TransactionalEventListener`, `@ApplicationModuleListener`) silently and Modulith registers no publication. Accepts `TransactionBoundary.inTransaction(...)` as the boundary.
- `DCA-USE-013` — transactional use cases must not call remote-capable output ports (anything but `Repository`, `Store`, `DomainEventPublisher`, `IntegrationEventPublisher`, `TransactionBoundary`): a remote round trip inside the transaction holds the connection and cannot be rolled back. Catalog: 109 rules.
