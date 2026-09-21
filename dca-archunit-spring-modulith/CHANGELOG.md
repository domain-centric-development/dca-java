# Changelog — dca-archunit-spring-modulith

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

## [0.2.0] - 2026-09-21

### Changed

- Rebuilt against `dca-archunit` **0.5.0**. No source change in this artifact: the published POM of
  `0.1.0` pins `dca-archunit 0.4.0`, so a project combining the Modulith verification with the
  current rule catalog got whichever version its build tool preferred — and with 0.4.0 it would
  silently run a catalog without the `errors` set and with three ids that are now retired. The
  dependency is now stated. `0.1.0` stays usable with `dca-archunit 0.4.0`.

## [0.1.0] - 2026-09-08

Depends on `dca-archunit` 0.3.0. `spring-modulith-core` is `compileOnly` (minimum 2.0; tested against
2.0.3 with Spring Boot 4.0.2) — a Modulith project has it through `spring-modulith-starter-test`.

### Added

- `DcaSpringModulithTest` — JUnit 5 base class shaped like `DcaArchitectureTest`: `moduleStructureIsValid()`
  runs `ApplicationModules.verify()`, a diagnostic test lists the discovered modules and named
  interfaces.
- `SpringModulithModules.of(DcaLayout)` — the filtered `ApplicationModules` for projects asserting
  themselves; `SpringModulithModules.testClasses()` / `isTestClass(String)` exclude `*Test`, `*Tests`, `*Spec`,
  `*IT` **and their nested and closure classes** by full name, so architecture tests in the base package
  do not become a synthetic root module.
