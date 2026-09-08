package dev.domaincentric.dca.spring;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import java.time.Instant;
import java.util.UUID;

/** Minimal aggregate and event for the adapter tests. */
final class Fixtures {

  private Fixtures() {}

  record OrderId(UUID value) implements Id {}

  record OrderPlaced(UUID eventId, Instant occurredOn, String orderId) implements DomainEvent {
    static OrderPlaced of(String orderId) {
      return new OrderPlaced(UUID.randomUUID(), Instant.now(), orderId);
    }
  }

  static final class Order extends BaseAggregateRoot<Order, OrderId> {
    private final OrderId id = new OrderId(UUID.randomUUID());

    @Override
    public OrderId id() {
      return id;
    }

    void place() {
      registerEvent(OrderPlaced.of(id.value().toString()));
    }
  }
}
