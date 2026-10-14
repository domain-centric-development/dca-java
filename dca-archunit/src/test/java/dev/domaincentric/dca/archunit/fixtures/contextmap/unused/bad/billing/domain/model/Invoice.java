package dev.domaincentric.dca.archunit.fixtures.contextmap.unused.bad.billing.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Invoice(String number, long totalCents) implements Value {}
