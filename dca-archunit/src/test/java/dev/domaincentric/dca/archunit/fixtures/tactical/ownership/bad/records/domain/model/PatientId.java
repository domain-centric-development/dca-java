package dev.domaincentric.dca.archunit.fixtures.tactical.ownership.bad.records.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record PatientId(String value) implements Id, Value {}
