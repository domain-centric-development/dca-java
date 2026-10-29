package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** A wrapper value: its one component may be the primitive it wraps. */
public record PostalCode(String value) implements Value {
  public static PostalCode of(String value) {
    return new PostalCode(value);
  }
}
