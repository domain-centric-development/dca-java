package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Entity;

public final class OrderLineItem implements Entity<OrderLineItem, OrderId> {
  private final OrderId id;

  public OrderLineItem(OrderId id) {
    this.id = id;
  }

  @Override
  public OrderId id() {
    return id;
  }
}
