package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** A value of several components: each one is a value too - the street and the city are not. */
public record Address(String street, PostalCode postalCode, String city) implements Value {}
