# dca-java

Java libraries for **Domain-Centric Architecture (DCA)** — a synthesis of Domain-Driven Design,
Hexagonal Architecture and Clean Architecture.

*Written with AI assistance — drafted mainly by Claude, reviewed and directed by Christoph Bloemer since
2025. The architecture rules in this repository's build are part of how that work is verified.*

| Artifact | What it is | Dependencies |
|----------|------------|--------------|
| `dev.domaincentric:dca-building-blocks` | The building blocks your code implements: DDD tactical markers (`AggregateRoot`, `Entity`, `Value`, `DomainEvent`, …), strategic annotations (`@BoundedContext`, `@SharedKernel`, `@Upstream`, `@Partnership`, …) and hexagonal port interfaces (`UseCase`, `Repository`, `Store`, …), the application-layer `TransactionBoundary` and the two exception base types (`DomainException`, `UseCaseException`) | none |
| `dev.domaincentric:dca-archunit` | The governance rules: 119 ArchUnit rules pinned to those building blocks, plus an executable context map | `dca-building-blocks`, ArchUnit |
| `dev.domaincentric:dca-spring` | The runtime adapters the rules demand: `SpringDomainEventPublisher` (over `ApplicationEventPublisher`), `SpringTransactionBoundary` (over `TransactionTemplate`), an `InMemoryTransactionBoundary` for tests, and a Spring Boot auto-configuration | `dca-building-blocks`; Spring `compileOnly` — your Boot BOM pins the version |
| `dev.domaincentric:dca-archunit-spring-modulith` | Spring Modulith's module verification as a DCA test: `DcaSpringModulithTest` next to `DcaArchitectureTest`, with the test-class exclusion Modulith needs | `dca-archunit`; `spring-modulith-core` `compileOnly` |

A Spring / Spring Modulith project ends up with two production and two test dependencies; every one
of them is optional except the building blocks:

```kotlin
dependencies {
    implementation("dev.domaincentric:dca-building-blocks:0.3.0")
    implementation("dev.domaincentric:dca-spring:0.2.0")
    testImplementation("dev.domaincentric:dca-archunit:0.5.0")
    testImplementation("dev.domaincentric:dca-archunit-spring-modulith:0.2.0")   // Spring Modulith projects only
}
```

A Spring project without Modulith drops the last line: the module verification is all that artifact
holds, and it needs `spring-modulith-core` on the test class path. Every other framework — Jakarta EE,
Quarkus, Micronaut, hand-wired — uses `dca-building-blocks` + `dca-archunit` with the matching
`FrameworkAnnotations` preset and no satellite at all.

Both target Java 17+ — built with a Java 21 toolchain at `--release 17`, and the test suite runs on a
Java 17 runtime as well (`./gradlew test -PjavaToolchain=17`, part of CI). Versions are independent;
see [Versioning](#versioning).

## Quick start

### 1. Building blocks in production code

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.domaincentric:dca-building-blocks:0.3.0")
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
│                                Factory, Specification, DomainException
├── ddd.strategic                @BoundedContext
├── ddd.strategic.relationships  @SharedKernel, @OpenHostService, @Upstream, @ExternalUpstream, @Partnership
├── hexagonal.port.in / .out     InputPort, UseCase  |  OutputPort, Repository, Store,
│                                DomainEventPublisher, IntegrationEventPublisher
└── application                  TransactionBoundary (execution abstraction, not a port),
                                 UseCaseException
```

### 2. Rules in an architecture test

```kotlin
dependencies {
    implementation("dev.domaincentric:dca-building-blocks:0.3.0")

    testImplementation("dev.domaincentric:dca-archunit:0.5.0")
    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test { useJUnitPlatform() }
```

All five lines are needed: without the BOM the Jupiter dependency has no version, without
`useJUnitPlatform()` Gradle finds no test, and without the launcher the test executor does not
start. This is the block of
[`samples/minimal-consumer/build.gradle.kts`](samples/minimal-consumer/build.gradle.kts), which CI
keeps green against a freshly published local build.

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
    .withModelSubpackage("entities")          // domain/entities instead of domain/model
    .withIncomingEventSubpackage("listener")  // event consumers under adapter/incoming/listener (default: event)
    .withUseCaseSuffix("ApplicationService")
    .withControllerSuffix("Page")
    .allowingInDomain("org.jmolecules..")     // extra third-party packages allowed in the domain
    .withFrameworkAnnotations(FrameworkAnnotations.jakarta());  // only if detection is wrong for you
```

The rules never name a framework — they resolve *roles* (`injectable`, `webController`, `restController`,
`transactional`, `eventListener`, `moduleDeclaration`, `publishedInterface`, `persistenceEntity`) through
`FrameworkAnnotations`. Usually you write nothing: `DcaLayout.forBasePackage` **detects** the framework on the
test class path and picks the preset (`spring`, `jakarta`, `quarkus`, `micronaut`; Spring when nothing is found),
and the report's first line says which and why — `framework annotations: quarkus (detected; also jakarta)`,
`spring (default)`, or `spring (default; undecided: micronaut, quarkus)` on a mixed class path where nothing is
chosen. To choose by hand: `dca.framework=micronaut` in `dca-archunit.properties`, or in code a preset
adjusted to your platform (`FrameworkAnnotations.jakarta().withRestController("com.acme.platform.Endpoint")`);
`none()` leaves every role empty for a hand-wired application. Explicit beats configured beats detected.

**Shipping a preset of your own.** A framework the library does not know — or your company platform — adds its
preset in a library of its own, without a change here: one class implementing
`dev.domaincentric.dca.archunit.spi.FrameworkAnnotationsProvider` (`name()`, `annotations()`, optionally
`detect(ClassLoader)` probing one class file and `priority()`), one line in
`META-INF/services/dev.domaincentric.dca.archunit.spi.FrameworkAnnotationsProvider`, and a dependency on
`dca-archunit` only. It is then detected, selectable by name and reported like a built-in
(`acme (detected)`). Presets are data; rules that need framework classes at test time stay in satellite artifacts
such as `dca-archunit-spring-modulith`.

### 4. Runtime adapters — `dca-spring`

`DCA-USE-009` demands that a use case publishes the saved aggregate's events, `DCA-USE-012` that every
save, delete and publication runs inside a transaction — a repository may write one aggregate as several
statements, and Spring's after-commit relays (`@TransactionalEventListener`,
`@ApplicationModuleListener`) are skipped **silently** without one. `dca-building-blocks` ships the
ports; `dca-spring` ships the two implementations every project used to copy:

```kotlin
implementation("dev.domaincentric:dca-spring:0.2.0")
```

In a Spring Boot **4** application with a transaction manager, nothing else is needed: the
auto-configuration registers `SpringDomainEventPublisher` and — once a `PlatformTransactionManager`
bean exists — `SpringTransactionBoundary`, each unless you define the port yourself. Without Boot,
register the two classes as beans. (The auto-configuration is Boot 4; on Boot 3.x the ordering it
relies on does not exist and nothing says so — see [Compatibility](#compatibility).)

**Without a transaction manager, three things are missing, and none of them announces itself.** An
in-memory application started from `spring-boot-starter` has no manager, and neither
`spring-boot-starter` nor `spring-modulith-starter-core` brings Boot's `TransactionAutoConfiguration`.
Add all three, visibly:

| Add | Otherwise |
|---|---|
| `org.springframework.boot:spring-boot-transaction` | `@Transactional` — which `DCA-USE-012` demands — does not compile: *package org.springframework.transaction.annotation does not exist* |
| a `PlatformTransactionManager` bean of your own | the context does not start: *required a bean of type TransactionBoundary that could not be found*, because `dcaTransactionBoundary` is conditional on a manager. `dca-spring` deliberately publishes no no-op manager |
| `org.springframework.modulith:spring-modulith-events-api` | `@ApplicationModuleListener` is not on the class path |

With `spring-tx` present but no manager, `@Transactional` compiles and does nothing: the relays never
fire while every rule stays green. `InMemoryTransactionBoundary` is for tests — same nesting contract,
no Spring.

**A default, not a prescription.** The rules check that a use case publishes through the
`DomainEventPublisher` port inside a transaction boundary — not which class stands behind the port.
`ApplicationEventPublisher` is the default because Spring's after-commit listeners and Spring Modulith
build on it. To choose differently: define your own `DomainEventPublisher` / `TransactionBoundary` bean
(the auto-configuration backs off per port), set `dca.spring.enabled=false` (nothing registered, the
classes remain usable by hand), or leave `dca-spring` out and keep only the building blocks. What
`dca-spring` deliberately does not provide is an `IntegrationEventPublisher`: outbox table, Modulith's
event publication registry or a broker is a project decision.

### 5. Spring Modulith verification — `dca-archunit-spring-modulith`

Modulith's `ApplicationModules.verify()` is not an ArchUnit rule and needs `spring-modulith-core` at
compile time, so it lives in its own optional artifact instead of `dca-archunit` (which stays
framework-free — a build check enforces it):

```kotlin
testImplementation("dev.domaincentric:dca-archunit-spring-modulith:0.2.0")
```

```java
class ModulithTest extends DcaSpringModulithTest {
  @Override
  protected DcaLayout layout() {
    return DcaLayout.forBasePackage("com.acme.shop");
  }
}
```

Two base classes, two test classes. The artifact's one piece of knowledge is the test-class filter:
architecture tests in the base package would otherwise become a synthetic root module that Modulith
reports as depending on non-exposed types — matched by full name, so inner and Groovy closure classes
(`FooTest$1`, `FooSpec$_check_closure1`) are excluded with their owner. `SpringModulithModules.of(layout)`
returns the filtered `ApplicationModules` for your own assertions.

### 6. Choose which rules run, and how strictly

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
cannot be frozen; freezing one fails with a message naming it. Lower those to `warning(...)` instead,
or scope them with `dca.rule.<id>.ignore`, which works for every rule and is the only one of the two
that `DomainCentric.ArchRules` also offers — it has no baseline dial yet, so a project that keeps one
configuration file for both stacks should prefer `ignore`.
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

## The smallest DCA

Nothing in the catalog fires on a concept the project has not declared: a rule with an empty
selection passes. So the first day needs seven building blocks, not thirty-one:

- `AggregateRoot<T, ID>` and `Id` — one aggregate and its identifier
- `Value` — the attributes that have no lifecycle of their own
- `Repository<T, ID>` — how that aggregate is loaded and stored
- `InputPort` — what the outside may ask the application to do
- `DomainException` — a broken rule of the model, named after the rule
- `@BoundedContext` on one `package-info.java` — so the rules know where the module begins

That is enough for `DcaArchitectureTest` to say something useful. `Entity`, `DomainEvent`,
`IntegrationEvent`, `DomainService`, `Factory`, `Specification`, `Store`, the publishers and the
context-map annotations each switch on the rules that govern them, when and only when the project
introduces the concept. The test source set's `fixtures/layout/greenfield` package is that shape,
nine files, and every rule of the catalog passes over it.

**Your own vocabulary instead of ours.** A code base that already has an aggregate base class keeps
it and points the roles at it, rather than migrating types or excluding rule ids:

```java
DcaLayout.forBasePackage("com.acme.billing")
    .withMarkers(
        DcaMarkers.dca()
            .named("acme")
            .withAggregateRoot("com.acme.common.Aggregate")
            .withRepository("com.acme.common.Store"));
```

Every rule then selects on those types, the vocabulary's own packages are excluded from the
third-party checks, and the test report names the vocabulary it resolved. One caveat:
`withOutputPort` must be set as soon as any other port role is, because `DCA-HEX-009` measures every
port against it.

**Domain events are optional; the marker's API is not.** `AggregateRoot<T, ID>` declares
`domainEvents()` and `clearDomainEvents()`, so a project that models no events still inherits
them. No rule requires an event to exist — every event rule passes on an empty selection — but if
the two methods are unwanted on the model, point the aggregate-root role at a type of your own; the
rules follow the role, not the base class.

**Your own names.** Where a rule finds something by name rather than by role — the use-case,
controller, REST-controller, aggregate-root, repository, store, factory and specification suffixes,
the segments of the package layout, and the types a domain event may store its occurrence time in
(`withTimestampTypes`) — `DcaLayout` has a `with…` for it. Configure the name; do not switch the
rule off.

## Rule catalog

**119 rule ids in 11 sets: 113 enforced, 6 informational.** Five further ids are retired and keep their meaning in the retirement list. The exact, current list is
[RULES.md](RULES.md) and [rules.json](rules.json), both generated from the code by
`./gradlew :dca-archunit:rulesCatalog` and verified in CI; every number in this README is taken from
there. `rules.json` names the library version its texts belong to.

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
| `errors` | `DCA-ERR` | domain and use-case exceptions: base types, their layer, no framework or transport vocabulary |

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

## Compatibility

| Runs on | Minimum | Built and tested against |
|---|---|---|
| Java | 17 | 17 bytecode, toolchain 21 and 25 |
| JUnit Jupiter | 5.10 | 5.10.2 and 6.1.3 |
| ArchUnit | 1.4 | 1.4.1 and 1.5.0 |
| Spring Boot (`dca-spring`) | **4.0** | 4.0.2 |
| Spring Framework (`dca-spring`) | 7.0 | 7.0.3 |
| Spring Modulith (`dca-archunit-spring-modulith`) | 2.0 | 2.0.3 |

`dca-building-blocks` and `dca-archunit` have no framework on their class path; the JUnit dependency of
`dca-archunit` is `compileOnly`, so the version above is the one your own build brings. `dca-spring`
registers its beans through Spring Boot 4 auto-configuration and does not work on Boot 3.x — the
ordering it relies on does not exist there, and nothing fails loudly, so pin Boot 4 or wire the beans
yourself.

## Public API

What a consumer may rely on, and what may change in a patch:

| Public — covered by the versioning promise | Internal — may change without notice |
|---|---|
| The **rule ids** and their meaning, `rules.json` and `RULES.md` | The rule-set classes (`TacticalPatternRules`, `UseCaseRules`, …) and their factory methods |
| `DcaLayout`, `DcaMarkers`, `FrameworkAnnotations` and the preset SPI | The `rules` package as a whole, and every helper in it |
| `DcaArchitecture`, `DcaRules`, `DcaRuleSelection`, `DcaRuleExecution`, `DcaRuleOutcome`, `DcaSeverity` | The wording of a violation message, beyond the `[DCA-XXX-nnn]` prefix every line carries |
| `DcaRule` as a type to read — id, title, rationale, `selects()`, `checks()` | The retired rules' implementation classes, which exist only so an old reference still compiles |
| `dev.domaincentric.dca.buildingblocks..` — every marker and port type | Test fixtures, and anything under a `catalog` or `spi.internal` package |
| The JUnit base class `DcaArchitectureTest` | |

The rule-set classes are `public` because the catalog generator and the JUnit integration are in a
different package, not because they are meant to be called directly. Build a run through `DcaRules`
and a `DcaRuleSelection`; that is the supported entry point, and it is the one that applies severities,
freezing and tolerated violations.

## Versioning

Semantic versioning, independent per artifact:

- `dca-building-blocks` — rarely changes; a new marker is a minor bump, a removed or renamed one a
  major bump.
- `dca-archunit` — **from 1.0 on, a new or tightened rule is a major bump.** Adding a rule can turn a
  green build red, and SemVer calls that breaking however small the change is; calling it a minor bump
  would make the number useless for exactly the consumers who pin it. A relaxed rule, a fixed false
  positive and a clearer message are patches. **Before 1.0** a minor version may add and tighten rules,
  which is what 0.x is for: every such change is listed under *What can turn a green build red* in the
  changelog, with a migration note at the top of the release. Either way, pin the version.
- **Rule ids are the stable contract.** An id is never reused for a different check and never
  renumbered. A withdrawn rule keeps its id in the retirement list of `rules.json` with the reason and
  its replacement, so a `dca.rules.off` entry or a catalog reference never silently means something
  else.
- `dca-spring`, `dca-archunit-spring-modulith` — ordinary SemVer on their own APIs; a raised minimum Spring or
  Modulith version is a minor bump.

Tags: `building-blocks/vX.Y.Z`, `archunit/vX.Y.Z`, `spring/vX.Y.Z`, `archunit-spring-modulith/vX.Y.Z` — one tag
per released artifact; see [RELEASING.md](RELEASING.md). `dca-archunit` and `dca-spring` depend on the
`dca-building-blocks` version named in `gradle.properties`, `dca-archunit-spring-modulith` on the `dca-archunit`
version named there, so those properties track the latest released versions. Changelogs:
[dca-building-blocks/CHANGELOG.md](dca-building-blocks/CHANGELOG.md), [dca-archunit/CHANGELOG.md](dca-archunit/CHANGELOG.md),
[dca-spring/CHANGELOG.md](dca-spring/CHANGELOG.md), [dca-archunit-spring-modulith/CHANGELOG.md](dca-archunit-spring-modulith/CHANGELOG.md).

## Build

```
./gradlew build                      # all four artifacts, all self-tests, framework-free check
./gradlew :dca-archunit:test         # rule self-tests against good/bad fixtures
./gradlew test -PjavaToolchain=17    # the same tests on the oldest supported runtime
./gradlew publishToMavenLocal        # try a snapshot in another project
```

`samples/minimal-consumer` is the consumer's view: `./gradlew test -PwithDcaJava` there runs it against
this checkout, `./gradlew test -PfromMavenLocal -PbuildingBlocksVersion=… -ParchunitVersion=…` against
what `publishToMavenLocal` produced — POM, module metadata and packaged license included.

Without a local JDK, the same through Docker (Podman works too): `docker compose run --rm build`
runs the build with a cached dependency volume, `docker compose run --rm catalog` renders
`rules.json`/`RULES.md`, and `docker build .` (or `docker compose --profile ci build`) is the CI-style
gate — the image only builds when everything is green and then carries the jars and the catalog under
`/out`. The tool services (`gradle`, `build`, `catalog`) live in the `tools` profile, so `docker compose
up` and `docker compose build` do nothing on their own; podman-compose does not activate a profile on
`run`, so there it is `podman-compose --profile tools run --rm build`.

Releases are published from a maintainer machine (`./scripts/release.sh <artifact> <version>`),
then tagged; the signing key never enters CI — [RELEASING.md](RELEASING.md).

## Author

**Christoph Bloemer** — [@chbloemer](https://github.com/chbloemer)

## License

MIT — see [LICENSE](LICENSE).

Contributions are accepted under the MIT licence, and the copyright holder may additionally publish
them under other licences (for example a documentation licence for prose).

## WP-39 parity update (2026-09-09)

Java building-blocks 0.2.0 makes event registration protected, matching the existing .NET visibility.
Factories delegate to aggregate creation. Both samples use Price, the six-field product-created v1 notification, ISO
money bounds/rounding, snapshot checkout and supplied-fact domain services. Role snapshots and invalid default quantities
are guarded. The independent shared specification has 45 vectors; final gate results are recorded in WP-39.
The specification stays unpublished and is an opt-in local check of both samples (not part of their builds).
