package dev.domaincentric.dca.archunit.fixtures.tactical.factories.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/**
 * An enum value with one factory of its own beside the language's valueOf(String), and two named
 * instances without parameters.
 */
public enum Currency implements Value {
  EUR,
  USD;

  public static Currency domestic() {
    return EUR;
  }

  public static Currency reporting() {
    return USD;
  }

  public static Currency fromCode(String code) {
    return valueOf(code.toUpperCase(java.util.Locale.ROOT));
  }
}
