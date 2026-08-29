package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.model;

import dev.domaincentric.dca.archunit.fixtures.advanced.good.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Factory;
import java.util.UUID;

public final class OrderFactory implements Factory {
  public Order create(Money total) {
    return new Order(new OrderId(UUID.randomUUID()), total);
  }
}
