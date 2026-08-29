package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record ShipmentId(String value) implements Id, Value {}
