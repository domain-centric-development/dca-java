package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder.OrderRepository;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

/** DCA-TAC-002 (repository field), DCA-TAC-003 (field of another aggregate root). */
public final class Order extends BaseAggregateRoot<Order, OrderId> {
  private final OrderId id;
  private final Customer customer;
  private final OrderRepository repository;

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

  public void persist() {
    repository.save(this);
  }
}
