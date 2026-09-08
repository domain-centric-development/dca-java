# Changelog — dca-spring

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

## [0.1.0] - 2026-09-08

Depends on `dca-building-blocks` 0.1.2. Spring Framework 7 / Spring Boot 4 are `compileOnly` — the
consumer's BOM pins them; built and tested against Spring 7.0.3 and Boot 4.0.2.

### Added

- `SpringDomainEventPublisher` — `DomainEventPublisher` over `ApplicationEventPublisher`. Dispatches the
  aggregate's events first and clears them afterwards, so a throwing listener leaves them in place and
  fails the use case.
- `SpringTransactionBoundary` — `TransactionBoundary` over `TransactionTemplate` with `REQUIRED`
  propagation; nested blocks join, an inner failure marks the shared transaction rollback-only
  (`UnexpectedRollbackException` at the outermost block).
- `InMemoryTransactionBoundary` — the same nesting contract without Spring, for tests and the
  in-memory phase.
- `DcaSpringAutoConfiguration` — registers the publisher, and the boundary once a
  `PlatformTransactionManager` exists, each unless the application defines the port itself. Ships no
  no-op transaction manager on purpose; its javadoc names the three things an in-memory application
  needs for after-commit relays to fire (`spring-boot-transaction`, a manager bean,
  `spring-modulith-events-api`).
