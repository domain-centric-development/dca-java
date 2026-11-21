package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record RiskRate(int perMille) implements Value {}
