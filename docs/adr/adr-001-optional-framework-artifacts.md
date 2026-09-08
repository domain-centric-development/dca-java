# ADR-001: Framework-bound code ships in optional artifacts, never in the building blocks or the rules

**Status:** Accepted — 2026-09-08

## Context

`dca-building-blocks` defines the ports a DCA application implements — `DomainEventPublisher`,
`TransactionBoundary` — and `dca-archunit` checks that use cases publish (`DCA-USE-009`) and do so inside a
transaction (`DCA-USE-012`). Neither artifact shipped an implementation, so every Spring project wrote the
same two classes; both reference implementations and a greenfield project had them three times over. The
Spring Modulith verification test was in the same position: not an ArchUnit rule, needs
`spring-modulith-core` at compile time, copied as a template into every project.

An experiment (2026-09-08, Spring Boot 4.0.2, Modulith 2.0.3) showed why the gap matters: without a
transaction manager `@Transactional` is silently inert — the context starts, no log line appears, the
after-commit relays never fire, the rules stay green. In Boot 4 even a manager bean alone is not enough,
because `TransactionAutoConfiguration` lives in `spring-boot-transaction`, which `spring-boot-starter` does
not include.

Two shapes were on the table: (A) publish the implementations as artifacts of this repository, or (B) let
the bootstrap tooling generate them into each project.

## Decision

**A, with a boundary.** Two optional artifacts, each depending on the framework `compileOnly` so the
consumer's BOM pins the version:

- `dca-spring` (runtime): `SpringDomainEventPublisher`, `SpringTransactionBoundary`,
  `InMemoryTransactionBoundary`, `DcaSpringAutoConfiguration`.
- `dca-archunit-spring-modulith` (test): `DcaSpringModulithTest`, `SpringModulithModules` with the test-class filter.

`dca-building-blocks` and `dca-archunit` stay framework-free; a build check (`verifyFrameworkFree`) fails
if Spring appears on their class paths. A consumer therefore pairs two production with two test
dependencies: building blocks + spring, archunit + archunit-spring-modulith.

**No no-op `PlatformTransactionManager` is published.** The in-memory phase needs one, but a published
no-op is a footgun that survives into production. The auto-configuration's javadoc, the README and the
knowledge catalog name the three items instead — `spring-boot-transaction`, a manager bean written in the
project, `spring-modulith-events-api` — so the placeholder is visible code with a visible replacement point.

## Consequences

- Reference implementations delete their private copies and consume the artifacts (they cannot keep a
  private copy of a published class).
- Bootstrap tooling adds dependencies instead of templates; its last generated architecture-test template
  goes away.
- Runtime and test scope never share an artifact: `dca-spring` would otherwise pull ArchUnit into
  production, or the Modulith test would pull runtime adapters into test scope.
- A Quarkus or Micronaut twin of `dca-spring` is possible later without touching the building blocks;
  the artifact is named after the framework for that reason.
- .NET stays template-only for now: no single framework equivalent of `ApplicationEventPublisher`, and
  the .NET sample uses its own `Channel<T>` plumbing. Recorded in the planning's porting status.
