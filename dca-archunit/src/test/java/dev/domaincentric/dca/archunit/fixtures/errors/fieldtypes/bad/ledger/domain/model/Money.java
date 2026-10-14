package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Money(long cents) implements Value {}
