package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record PolicyId(String value) implements Id, Value {}
