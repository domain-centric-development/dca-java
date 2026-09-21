package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/**
 * An enum value object with constant-specific class bodies. The compiler makes such an enum
 * abstract, so DCA-TAC-009 could never be satisfied - which is why it does not select enums.
 */
public enum Currency implements Value {
  EUR {
    @Override
    public String symbol() {
      return "€";
    }
  },
  USD {
    @Override
    public String symbol() {
      return "$";
    }
  };

  public abstract String symbol();
}
