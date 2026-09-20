# ADR-002: Two exception base types in the building blocks, and what a rule can decide about them

**Status:** Accepted — 2026-09-20

## Context

The architectural doctrine names three layers of failure: a broken business rule raised by the model, a
use-case failure the application reports to its callers, and the translation into a protocol answer in the
incoming adapter. Neither library carried anything for it — no base type in `dca-building-blocks`, no rule
in the `dca-archunit` catalog. Without a type to select on, a rule cannot exist, so the doctrine was
checkable by review only, and both reference implementations had drifted: business rules surfaced as the
platform's own argument and state exceptions, which an adapter cannot tell apart from a programming error.

Two shapes were on the table: (A) two abstract base classes plus the rules that pin them, or (B) soften the
doctrine to "the platform's exceptions in guards, translation in the adapter". (B) carries no rule — nothing
in it is selectable — and was rejected for that reason.

## Decision

**A.** `DomainException` in `ddd.tactical` and `UseCaseException` in `application`, both abstract, both
unchecked, each with the two usual constructors and no field of their own. They are the first building
blocks that are classes rather than interfaces or annotations: a failure has to *be* a throwable, so a
marker interface cannot carry the contract.

**One word in both stacks: `UseCaseException`.** The doctrine's own term is "application exception", but the
.NET platform occupies `ApplicationException` and discourages its use. One name that reads the same in both
libraries is worth more than following the layer's name in one of them; the guide's mapping table carries
the term.

**No code or status field on either type.** A machine-readable code would be the adapter's decision taken in
the inner layer, which is exactly what the types exist to prevent. The adapter switches on the exception
type instead — that is what one type per outcome is for.

**Five enforced rules and one diagnostic** (`DCA-ERR-001` … `DCA-ERR-006`, same ids in `dca-dotnet`): the two
base types are used where they belong (001, 003), a subtype resides in the layer its base type names (002),
no configured framework metadata on either (004), and no technical or transport word in the name (005).

**What the catalog deliberately does not check.** A `throw` site is not part of the import model, so no rule
can decide whether a specific failure should have been a domain exception rather than an argument guard, and
none can see a `catch` block. `DCA-ERR-006` is therefore informational: it lists incoming adapters that drive
an input port and name neither failure type, and it never fails. The semantic questions — a blanket catch, a
business rule raised as a state exception — stay with the review perspectives.

## Consequences

- Argument guards keep the platform's own exceptions. The rules do not select them, and the guide states the
  criterion: a failure a domain expert has a word for is a domain exception, a contract for the caller is not.
- A project adopting the catalog on an existing code base will see `DCA-ERR-001`/`-003` fire for every
  exception it declares in an inner layer; both are lowerable to `WARN` like any other rule.
- The `errors` set is the eleventh, bringing the catalog to 120 entries; `dca-dotnet` carries the same ids.
- A transport-status annotation on an exception (the doctrine's own counter-example) is still uncaught:
  no role of `FrameworkAnnotations` classifies one. Adding that role is a separate change in both libraries.
