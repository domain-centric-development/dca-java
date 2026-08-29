package com.acme.shop.sharedkernel.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import java.util.UUID;

public record ProductId(UUID value) implements Id {
  public static ProductId generate() {
    return new ProductId(UUID.randomUUID());
  }
}
