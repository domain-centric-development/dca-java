# Changelog — dca-archunit

All notable changes to this artifact. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/); versioning: SemVer.

## [Unreleased]

### Added
- Initial extraction from the DCA reference implementation.
- `DCA-USE-012` — use cases that publish domain events must be transactional (`@Transactional` on class or method): without an active transaction Spring skips after-commit listeners (`@TransactionalEventListener`, `@ApplicationModuleListener`) silently and Modulith registers no publication. Catalog: 108 rules.
