package com.acme.shop.cart.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record Quantity(int amount) implements Value {
  public Quantity {
    if (amount <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }
}
