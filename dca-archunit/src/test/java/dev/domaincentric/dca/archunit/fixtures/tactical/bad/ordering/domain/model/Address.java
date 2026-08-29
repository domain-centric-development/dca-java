package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** DCA-TAC-008: value object holding an entity. */
public record Address(String street, Shipment shipment) implements Value {}
