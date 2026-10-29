package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** A value of several components, each a value object. */
public record Address(Street street, PostalCode postalCode, City city) implements Value {}
