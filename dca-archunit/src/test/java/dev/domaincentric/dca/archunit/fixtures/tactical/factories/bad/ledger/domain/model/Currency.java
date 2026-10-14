package dev.domaincentric.dca.archunit.fixtures.tactical.factories.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** An enum value with one factory of its own beside the language's valueOf(String). */
public enum Currency implements Value {
  EUR,
  USD;

  public static Currency fromCode(String code) {
    return valueOf(code.toUpperCase(java.util.Locale.ROOT));
  }
}
