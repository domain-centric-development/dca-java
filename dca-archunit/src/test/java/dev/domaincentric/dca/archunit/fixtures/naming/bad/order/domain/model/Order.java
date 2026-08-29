package dev.domaincentric.dca.archunit.fixtures.naming.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.UUID;

public final class Order extends BaseAggregateRoot<Order, OrderId> {
  private final OrderId id;

  private Order(OrderId id) {
    this.id = id;
  }

  public static Order place(OrderId id) {
    Order order = new Order(id);
    order.registerEvent(new OrderPlaced(UUID.randomUUID(), Instant.now(), id));
    return order;
  }

  @Override
  public OrderId id() {
    return id;
  }
}
