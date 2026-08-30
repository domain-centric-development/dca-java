# dca-java

Java libraries for **Domain-Centric Architecture (DCA)** — a synthesis of Domain-Driven Design,
Hexagonal Architecture and Clean Architecture.

| Artifact | What it is | Dependencies |
|----------|------------|--------------|
| `dev.domaincentric:dca-building-blocks` | The building blocks your code implements: DDD tactical markers (`AggregateRoot`, `Entity`, `Value`, `DomainEvent`, …), strategic annotations (`@BoundedContext`, `@SharedKernel`, `@Upstream`, `@Partnership`, …) and hexagonal port interfaces (`UseCase`, `Repository`, `Store`, …) and the application-layer `TransactionBoundary` | none |
| `dev.domaincentric:dca-archunit` | The governance rules: ~100 ArchUnit rules pinned to those building blocks, plus an executable context map | `dca-building-blocks`, ArchUnit |

Both target Java 17+. Versions are independent; see [Versioning](#versioning).

## Quick start

### 1. Building blocks in production code

```kotlin
// build.gradle.kts
dependencies {
    implementation("dev.domaincentric:dca-building-blocks:<version>")
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
    testImplementation("dev.domaincentric:dca-archunit:<version>")
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

That is the whole test. Every rule runs as its own dynamic test, named `[DCA-TAC-001] Aggregate roots must …`.

### 3. Adapt to your layout

```java
DcaLayout.forBasePackage("com.acme.shop")
    .withIncomingSubpackage("in")            // adapter/in instead of adapter/incoming
    .withOutgoingSubpackage("out")
    .withUseCaseSuffix("ApplicationService")
    .allowingInDomain("org.jmolecules..")     // extra third-party packages allowed in the domain
    .withFrameworkAnnotations(FrameworkAnnotations.spring());   // or your own FQNs
```

Switch rules off by id, or pick rule sets:

```java
@Override protected Set<String> excludedRuleIds() { return Set.of("DCA-NAM-004"); }
@Override protected List<DcaRule> rules() { return DcaRules.only(layout(), "tactical", "hexagonal"); }
```

Without JUnit's base class:

```java
DcaArchitecture arch = DcaArchitecture.load(DcaLayout.forBasePackage("com.acme.shop"));
DcaRules.checkAll(arch);                       // or iterate DcaRules.all(arch.layout())
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
  rule is a major bump, a relaxed rule or fixed false positive a patch.

Tags: `building-blocks/vX.Y.Z`, `archunit/vX.Y.Z`. Changelogs: [dca-building-blocks/CHANGELOG.md](dca-building-blocks/CHANGELOG.md), [dca-archunit/CHANGELOG.md](dca-archunit/CHANGELOG.md).

## Build

```
./gradlew build                      # both artifacts, all self-tests
./gradlew :dca-archunit:test         # rule self-tests against good/bad fixtures
./gradlew publishToMavenLocal        # try a snapshot in another project
```

## License

MIT — see [LICENSE](LICENSE).
