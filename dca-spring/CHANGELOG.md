# Changelog — dca-spring

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

## [0.3.0] - 2026-11-07

### Added

- **A returned refusal rolls back (WP-171).** `SpringTransactionBoundary` marks the status rollback-only when the
  work returns a `Refusal` (from `dca-building-blocks`) and returns it without throwing; marked locally, the
  rollback raises no `UnexpectedRollbackException`, also after a caught inner failure. `InMemoryTransactionBoundary`
  keeps the same contract and answers `lastBlockRolledBack()` for tests.
- `RefusalRollbackInterceptor` and `RefusalRollbackPostProcessor`: a `@Transactional` method that returns a
  `Refusal` rolls back. The post-processor wraps the advice of the transaction advisor that
  `@EnableTransactionManagement` registers, so the check runs between the method and the commit whatever else is
  ordered around it (Boot registers the transaction advisor with the lowest precedence, so a second advisor could
  not be ordered inside it). Registered by `DcaSpringAutoConfiguration`, **on by default**, switched off with
  `dca.spring.refusal-rollback.enabled=false`. AspectJ-woven transactions have no advisor and are not covered.

## [0.2.0] - 2026-09-21

### Changed

- Rebuilt against `dca-building-blocks` **0.3.0**. No source change in this artifact: the published
  POM of `0.1.0` pins `dca-building-blocks 0.2.0`, so a project combining `dca-spring` with the
  current building blocks relied on the build tool resolving the conflict in favour of the newer
  version. The dependency is now stated rather than resolved. `0.1.0` stays usable with
  `dca-building-blocks 0.2.0`.

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
  `PlatformTransactionManager` exists, each unless the application defines the port itself;
  `dca.spring.enabled=false` switches it off entirely. Ships no
  no-op transaction manager on purpose; its javadoc names the three things an in-memory application
  needs for after-commit relays to fire (`spring-boot-transaction`, a manager bean,
  `spring-modulith-events-api`).
