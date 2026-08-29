package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;

public record OrderId(String value) implements Id {}
