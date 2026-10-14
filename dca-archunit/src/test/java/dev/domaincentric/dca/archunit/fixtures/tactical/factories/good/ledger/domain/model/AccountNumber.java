package dev.domaincentric.dca.archunit.fixtures.tactical.factories.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** Two named origins with equal parameters: each name says which checks run. */
public record AccountNumber(String value) implements Value {

  public static AccountNumber fromInput(String input) {
    String normalized = input.strip().toUpperCase(java.util.Locale.ROOT);
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("empty");
    }
    return new AccountNumber(normalized);
  }

  public static AccountNumber fromStored(String stored) {
    return new AccountNumber(stored);
  }
}
