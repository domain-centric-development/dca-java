package dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.UUID;

public class Order extends BaseAggregateRoot<Order, OrderId> {
  private final OrderId id;

  private Order(OrderId id) {
    this.id = id;
  }

  public static Order place() {
    Order order = new Order(new OrderId(UUID.randomUUID()));
    order.registerEvent(new OrderPlaced(UUID.randomUUID(), Instant.now(), order.id));
    return order;
  }

  @Override
  public OrderId id() {
    return id;
  }
}
