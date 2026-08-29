package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class Order extends BaseAggregateRoot<Order, OrderId> {
  private final OrderId id;
  private final CustomerId customerId;
  private final List<LineItem> lineItems = new ArrayList<>();
  private boolean placed;

  public Order(OrderId id, CustomerId customerId) {
    this.id = id;
    this.customerId = customerId;
  }

  @Override
  public OrderId id() {
    return id;
  }

  public CustomerId customerId() {
    return customerId;
  }

  public List<LineItem> lineItems() {
    return List.copyOf(lineItems);
  }

  public void addLineItem(LineItemId lineItemId, String sku, Quantity quantity) {
    lineItems.add(new LineItem(lineItemId, sku, quantity));
  }

  public void place() {
    if (lineItems.isEmpty()) {
      throw new IllegalStateException("Cannot place an empty order");
    }
    placed = true;
    registerEvent(new OrderPlaced(UUID.randomUUID(), Instant.now(), id));
  }

  public boolean isPlaced() {
    return placed;
  }
}
