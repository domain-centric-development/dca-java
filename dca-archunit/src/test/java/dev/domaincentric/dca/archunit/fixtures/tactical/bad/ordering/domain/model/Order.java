package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder.OrderRepository;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.util.List;
import java.util.Map;

/**
 * DCA-TAC-002 (repository field), DCA-TAC-003 (field of another aggregate root, and another one
 * hidden in a nested container).
 */
public final class Order extends BaseAggregateRoot<Order, OrderId> {
  private final OrderReference linkedOrder = null;
  private final java.util.function.Supplier<Order> suppliedOrder = () -> null;
  private final OrderId id;
  private final Customer customer;
  private final OrderRepository repository;
  private final Map<String, List<Customer>> customersByRegion = Map.of();

  public Order(OrderId id, Customer customer, OrderRepository repository) {
    this.id = id;
    this.customer = customer;
    this.repository = repository;
  }

  @Override
  public OrderId id() {
    return id;
  }

  public Customer customer() {
    return customer;
  }

  public Map<String, List<Customer>> customersByRegion() {
    return customersByRegion;
  }

  public void persist() {
    repository.save(this);
  }
}
