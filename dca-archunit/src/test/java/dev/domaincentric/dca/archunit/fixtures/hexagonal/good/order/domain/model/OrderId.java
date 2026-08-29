package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;

public record OrderId(String value) implements Id {}
