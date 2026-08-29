package dev.domaincentric.dca.archunit.fixtures.contextmap.good.sharedkernel.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Money(long cents) implements Value {}
