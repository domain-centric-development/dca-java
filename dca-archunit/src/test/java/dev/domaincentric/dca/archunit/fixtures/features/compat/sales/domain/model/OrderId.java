package dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;
import java.util.UUID;

public record OrderId(UUID value) implements Id, Value {}
