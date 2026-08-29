package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.domain.model;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.UUID;

public class Order extends BaseAggregateRoot<Order, OrderId> {
  private final OrderId id;
  private final Money total;

  public Order(OrderId id, Money total) {
    this.id = id;
    this.total = total;
    registerEvent(new OrderPlaced(UUID.randomUUID(), Instant.now(), id));
  }

  @Override
  public OrderId id() {
    return id;
  }

  public Money total() {
    return total;
  }
}
