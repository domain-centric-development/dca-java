package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Holder(String value) implements Value {}
