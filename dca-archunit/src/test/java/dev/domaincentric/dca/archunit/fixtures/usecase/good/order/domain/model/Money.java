package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Money(long cents, String currency) implements Value {}
