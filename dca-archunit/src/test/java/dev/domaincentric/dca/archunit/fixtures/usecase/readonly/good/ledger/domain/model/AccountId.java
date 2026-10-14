package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record AccountId(String value) implements Id, Value {}
