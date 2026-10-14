package dev.domaincentric.dca.archunit.fixtures.contextmap.unused.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Entry(String account, long cents) implements Value {}
