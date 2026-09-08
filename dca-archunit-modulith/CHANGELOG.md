# Changelog — dca-archunit-modulith

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

## [0.1.0] - 2026-09-08

Depends on `dca-archunit` 0.3.0. `spring-modulith-core` is `compileOnly` (minimum 2.0; tested against
2.0.3 with Spring Boot 4.0.2) — a Modulith project has it through `spring-modulith-starter-test`.

### Added

- `DcaModulithTest` — JUnit 5 base class shaped like `DcaArchitectureTest`: `moduleStructureIsValid()`
  runs `ApplicationModules.verify()`, a diagnostic test lists the discovered modules and named
  interfaces.
- `ModulithModules.of(DcaLayout)` — the filtered `ApplicationModules` for projects asserting
  themselves; `ModulithModules.testClasses()` / `isTestClass(String)` exclude `*Test`, `*Tests`, `*Spec`,
  `*IT` **and their nested and closure classes** by full name, so architecture tests in the base package
  do not become a synthetic root module.
