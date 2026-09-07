package dev.domaincentric.dca.buildingblocks.ddd.tactical;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** The executable part of the building blocks: event collection on an aggregate root. */
class BaseAggregateRootTest {

  record OrderId(UUID value) implements Id {}

  record OrderPlaced(UUID eventId, Instant occurredOn, OrderId orderId) implements DomainEvent {}

  static final class Order extends BaseAggregateRoot<Order, OrderId> {
    private final OrderId id;

    Order(OrderId id) {
      this.id = id;
    }

    void place() {
      registerEvent(new OrderPlaced(UUID.randomUUID(), Instant.now(), id));
    }

    @Override
    public OrderId id() {
      return id;
    }
  }

  @Test
  void collectsRegisteredEventsInOrder() {
    Order order = new Order(new OrderId(UUID.randomUUID()));
    order.place();
    order.place();

    List<DomainEvent> events = order.domainEvents();
    assertEquals(2, events.size());
    assertTrue(events.get(0) instanceof OrderPlaced);
  }

  @Test
  void clearsEventsAfterPublication() {
    Order order = new Order(new OrderId(UUID.randomUUID()));
    order.place();

    order.clearDomainEvents();

    assertTrue(order.domainEvents().isEmpty());
  }

  @Test
  void exposedEventsAreReadOnly() {
    Order order = new Order(new OrderId(UUID.randomUUID()));
    order.place();

    assertThrows(UnsupportedOperationException.class, () -> order.domainEvents().clear());
  }

  @Test
  void rejectsANullEvent() {
    Order order = new Order(new OrderId(UUID.randomUUID()));

    assertThrows(IllegalArgumentException.class, () -> order.registerEvent(null));
  }

  @Test
  void identityComparisonUsesTheId() {
    OrderId id = new OrderId(UUID.randomUUID());
    Order one = new Order(id);
    Order same = new Order(id);
    Order other = new Order(new OrderId(UUID.randomUUID()));

    assertTrue(one.sameIdentityAs(same));
    assertFalse(one.sameIdentityAs(other));
    assertFalse(one.sameIdentityAs(null));
  }
}
