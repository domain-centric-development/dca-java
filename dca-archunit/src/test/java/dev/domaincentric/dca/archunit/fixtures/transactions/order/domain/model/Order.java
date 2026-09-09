package dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

public final class Order extends BaseAggregateRoot<Order, OrderId> {
  private final OrderId id;

  public Order(OrderId id) {
    this.id = id;
  }

  @Override
  public OrderId id() {
    return id;
  }

  public void recordChange() {
    registerEvent(new Changed(java.util.UUID.randomUUID(), java.time.Instant.EPOCH));
  }

  private record Changed(java.util.UUID eventId, java.time.Instant occurredOn)
      implements dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent {}
}
