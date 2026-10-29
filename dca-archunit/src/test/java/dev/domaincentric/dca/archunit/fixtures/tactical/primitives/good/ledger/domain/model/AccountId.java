package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;

public record AccountId(String value) implements Id {}
