# dca-java

Java libraries for **Domain-Centric Architecture (DCA)** — a synthesis of Domain-Driven Design,
Hexagonal Architecture and Clean Architecture.

*Written with AI assistance — drafted mainly by Claude, reviewed and directed by the author since
2025. The architecture rules in this repository's build are part of how that work is verified.*

| Artifact | What it is | Dependencies |
|----------|------------|--------------|
| `dev.domaincentric:dca-building-blocks` | The building blocks your code implements: DDD tactical markers (`AggregateRoot`, `Entity`, `Value`, `DomainEvent`, …), strategic annotations (`@BoundedContext`, `@SharedKernel`, `@Upstream`, `@Partnership`, …) and hexagonal port interfaces (`UseCase`, `Repository`, `Store`, …) and the application-layer `TransactionBoundary` | none |
| `dev.domaincentric:dca-archunit` | The governance rules: ~110 ArchUnit rules pinned to those building blocks, plus an executable context map | `dca-building-blocks`, ArchUnit |

Both target Java 17+ — built with a Java 21 toolchain at `--release 17`, and the test suite runs on a
Java 17 runtime as well (`./gradlew test -PjavaToolchain=17`, part of CI). Versions are independent;
see [Versioning](#versioning).

## Quick start

### 1. Building blocks in production code

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.domaincentric:dca-building-blocks:0.1.1")
}
```

```java
package com.acme.shop.cart.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

public class ShoppingCart extends BaseAggregateRoot<ShoppingCart, CartId> { … }
```

```java
// com/acme/shop/cart/package-info.java
@BoundedContext(name = "Shopping Cart", description = "Carts and their items")
@Upstream(context = "product", translation = Upstream.Translation.ANTI_CORRUPTION_LAYER,
          via = Upstream.Consumes.API, rationale = "Product data is translated into cart's own types")
package com.acme.shop.cart;
```

Packages:

```
dev.domaincentric.dca.buildingblocks
├── ddd.tactical                 AggregateRoot, BaseAggregateRoot, Entity, Value, Id, DomainEvent,
│                                IntegrationEvent, IntegrationEventType, DomainService, DomainGateway,
│                                Factory, Specification
├── ddd.strategic                @BoundedContext
├── ddd.strategic.relationships  @SharedKernel, @OpenHostService, @Upstream, @ExternalUpstream, @Partnership
├── hexagonal.port.in / .out     InputPort, UseCase  |  OutputPort, Repository, Store,
│                                DomainEventPublisher, IntegrationEventPublisher
└── application                  TransactionBoundary (execution abstraction, not a port)
```

### 2. Rules in an architecture test

```kotlin
dependencies {
    testImplementation("dev.domaincentric:dca-archunit:0.2.0")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
```

```java
package com.acme.shop;

import dev.domaincentric.dca.archunit.DcaLayout;
import dev.domaincentric.dca.archunit.junit.DcaArchitectureTest;

class ArchitectureTest extends DcaArchitectureTest {
  @Override
  protected DcaLayout layout() {
    return DcaLayout.forBasePackage("com.acme.shop");
  }
}
```

That is the whole test. Rules run as dynamic tests grouped by rule set, each named
`[DCA-TAC-001] Aggregate roots must …`.

### 3. Adapt to your layout

```java
DcaLayout.forBasePackage("com.acme.shop")
    .withIncomingSubpackage("in")            // adapter/in instead of adapter/incoming
    .withOutgoingSubpackage("out")
    .withApiSubpackage("contract")            // published sync contract (default: api); events: withEventsSubpackage
    .withUseCaseSuffix("ApplicationService")
    .withControllerSuffix("Page")
    .allowingInDomain("org.jmolecules..")     // extra third-party packages allowed in the domain
    .withFrameworkAnnotations(FrameworkAnnotations.spring());   // or your own FQNs
```

### 4. Choose which rules run, and how strictly

The catalog is opinionated, and no team adopts all of it on day one. A rule you disagree with, or
cannot satisfy yet, is a decision to record — not a reason to drop the library. `DcaRuleSelection`
expresses four things:

```java
@Override
protected DcaRuleSelection additionalSelection() {
  return DcaRuleSelection.all()
      .onlySets("cycles", "layered", "hexagonal")            // scope: adopt in stages
      .excluding("DCA-NAM-002", "no DI framework here")      // off, with the reason
      .warning("DCA-TAC-009", "made final step by step")     // reported, does not fail the build
      .ignoringViolationsMatching("DCA-STR-003", ".*legacy.*")   // a documented exception
      .frozen("DCA-ONI-002")                                 // baseline: only new violations fail
      .withFreezeStore(Path.of("arch/frozen"));
}
```

A rule that is switched off or lowered to a warning **stays in the report**, aborted with the reason
you recorded — so the decision remains visible instead of vanishing from the run.

The same configuration can live in `dca-archunit.properties` on the test class path, which the base
class reads and `additionalSelection()` is then applied on top of. No Java needed to tune it:

```properties
dca.rules.sets              = cycles,layered,hexagonal
dca.rules.off               = DCA-NAM-002
dca.rule.DCA-NAM-002.reason = no DI framework in this project
dca.rules.warn              = DCA-TAC-009
dca.rules.warn.sets         = naming
dca.rule.DCA-STR-003.ignore = .*legacy.*
dca.rule.DCA-STR-003.ignore.1 = Generated.{1,3}Client
dca.rules.freeze            = DCA-ONI-002
dca.rules.freeze.store      = arch/frozen
```

An unknown rule id or set name fails the run immediately — a typo must never leave a rule silently
enforced. The value of an `.ignore` key is **one** regular expression, commas included (`{1,3}` is a
quantifier, not a separator); a second expression for the same rule goes into an indexed key
(`.ignore.1`, `.ignore.2`, …). Rule-id and set lists stay comma-separated.

Both sources combine: the file is the base, `additionalSelection()` is merged on top, and the later
entry wins per rule id. Override `additionalSelection()`, not `selection()` — the latter *replaces*
the file, so a `dca-archunit.properties` added later would be ignored without a word.

**Two limitations.** Freezing needs a single ArchUnit rule to build the baseline from. The rules that
run several checks internally — the context-map set and those iterating over bounded contexts —
cannot be frozen; freezing one fails with a message naming it. Lower those to `warning(...)` instead.
And the transaction rules (`DCA-USE-009`, `-012`, `-013`) reason per *entry path*, following the directed
calls within the use case class: an entry point (a method callable from outside the class, or one nothing
in the class calls) may save through one helper and publish through another, and from every entry point no
route down to a publication may be free of `@Transactional` or `TransactionBoundary.inTransaction(...)` —
a helper two entry methods share does not connect them, an annotated caller does not cover another path to
the same helper, a publishing wrapper does not cover a direct call of the public method it wraps, and a
boundary on one route does not cover a second route to the same publisher. What they cannot see: ArchUnit folds a lambda's body into the enclosing method (the synthetic
`lambda$…` methods are not code units of the imported class), so whether a call sits *inside* the block
handed to `inTransaction`, in which order two calls run, and whether save and publication concern the
same aggregate are review checks.

Without JUnit's base class:

```java
DcaArchitecture arch = DcaArchitecture.load(DcaLayout.forBasePackage("com.acme.shop"));
DcaRules.checkAll(arch);                       // or checkAll(arch, selection) with a selection
```

## Rule catalog

Rule sets and identifier prefixes:

| Set | Prefix | Covers |
|-----|--------|--------|
| `layered` | `DCA-LAY` | layer dependency direction (domain ← application ← adapter ← infrastructure) |
| `onion` | `DCA-ONI` | onion view: nothing inside depends on anything outside |
| `hexagonal` | `DCA-HEX` | ports and adapters: input/output ports, adapter direction, no infrastructure leaks |
| `tactical` | `DCA-TAC` | aggregates, entities, value objects, ids, domain events, repositories |
| `strategic` | `DCA-STR` | bounded-context isolation, shared kernel, open host services, integration events |
| `contextmap` | `DCA-MAP` | `@Upstream` / `@Partnership` / `@ExternalUpstream` declarations ⇔ real dependencies |
| `advanced` | `DCA-ADV` | domain services, factories, specifications, event hygiene, integration event types |
| `usecase` | `DCA-USE` | one use case per package, `*InputPort` / `*UseCase` / `*Command` / `*Result` shape |
| `naming` | `DCA-NAM` | naming conventions, forbidden technical suffixes and bucket packages |
| `cycles` | `DCA-CYC` | no cycles between contexts, layers, use-case packages |

The full list with rationale is in [RULES.md](RULES.md) (generated from the code — `./gradlew :dca-archunit:rulesCatalog`).

## Beyond rules: executable context map

The same `@Upstream` / `@ExternalUpstream` / `@Partnership` declarations that the `contextmap` rules
verify can be rendered into a markdown context map (tables + Mermaid diagram). Rendering is **opt-in**
— call it from a test of your own:

```java
@Test
void renderContextMap() {
  ContextMapRenderer.of(architecture())
      .includeExternalSystems(true)
      .includePlanned(true)
      .writeTo(Path.of("docs/context-map.md"));
}
```

Because the rules guarantee declarations match the code, the rendered map cannot drift.

## Versioning

Semantic versioning, independent per artifact:

- `dca-building-blocks` — rarely changes; a new marker is a minor bump, a removed or renamed one a
  major bump.
- `dca-archunit` — a new rule is a minor bump (it can fail your build — pin versions), a tightened
  rule is a major bump, a relaxed rule or fixed false positive a patch. **Before 1.0** a minor version
  may add and tighten rules as well; every such change is listed under *Changed — breaking* in the
  changelog, with a migration note at the top of the release.

Tags: `building-blocks/vX.Y.Z`, `archunit/vX.Y.Z` — one tag per released artifact; see
[RELEASING.md](RELEASING.md). `dca-archunit` depends on the `dca-building-blocks` version named in
`gradle.properties`, so that property tracks the latest released marker version. Changelogs: [dca-building-blocks/CHANGELOG.md](dca-building-blocks/CHANGELOG.md), [dca-archunit/CHANGELOG.md](dca-archunit/CHANGELOG.md).

## Build

```
./gradlew build                      # both artifacts, all self-tests
./gradlew :dca-archunit:test         # rule self-tests against good/bad fixtures
./gradlew test -PjavaToolchain=17    # the same tests on the oldest supported runtime
./gradlew publishToMavenLocal        # try a snapshot in another project
```

`samples/minimal-consumer` is the consumer's view: `./gradlew test -PwithDcaJava` there runs it against
this checkout, `./gradlew test -PfromMavenLocal -PbuildingBlocksVersion=… -ParchunitVersion=…` against
what `publishToMavenLocal` produced — POM, module metadata and packaged license included.

Without a local JDK, the same through Docker (Podman works too): `docker compose run --rm build`
runs the build with a cached dependency volume, `docker compose run --rm catalog` renders
`rules.json`/`RULES.md`, and `docker build .` is the CI-style gate — the image only builds when
everything is green and then carries the jars and the catalog under `/out`.

Releases are published from a maintainer machine (`./scripts/release.sh <artifact> <version>`),
then tagged; the signing key never enters CI — [RELEASING.md](RELEASING.md).

## License

MIT — see [LICENSE](LICENSE).

Contributions are accepted under the MIT licence, and the copyright holder may additionally publish
them under other licences (for example a documentation licence for prose).
