package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

public final class Customer extends BaseAggregateRoot<Customer, CustomerId> {
  private final CustomerId id;

  public Customer(CustomerId id) {
    this.id = id;
  }

  @Override
  public CustomerId id() {
    return id;
  }
}
