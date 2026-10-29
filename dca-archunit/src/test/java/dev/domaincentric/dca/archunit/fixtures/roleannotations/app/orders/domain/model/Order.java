package dev.domaincentric.dca.archunit.fixtures.roleannotations.app.orders.domain.model;

/** Holds values only. */
public final class Order {
  private final Amount total;

  public Order(Amount total) {
    this.total = total;
  }

  public Amount total() {
    return total;
  }
}
