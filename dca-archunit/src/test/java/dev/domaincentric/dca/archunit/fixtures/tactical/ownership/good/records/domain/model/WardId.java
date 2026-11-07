package dev.domaincentric.dca.archunit.fixtures.tactical.ownership.good.records.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record WardId(String value) implements Id, Value {}
