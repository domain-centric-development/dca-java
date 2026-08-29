package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** Identity of an aggregate owned by another context — referenced by id only. */
public record CustomerId(String value) implements Id, Value {}
