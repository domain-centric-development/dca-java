package com.acme.shop.cart.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import java.util.UUID;

public record CartId(UUID value) implements Id {
  public static CartId generate() {
    return new CartId(UUID.randomUUID());
  }
}
