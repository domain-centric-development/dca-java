package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Entity;

public final class LineItem implements Entity<LineItem, LineItemId> {
  private final LineItemId id;
  private final String sku;
  private Quantity quantity;

  LineItem(LineItemId id, String sku, Quantity quantity) {
    this.id = id;
    this.sku = sku;
    this.quantity = quantity;
  }

  @Override
  public LineItemId id() {
    return id;
  }

  public String sku() {
    return sku;
  }

  public Quantity quantity() {
    return quantity;
  }

  void increase(Quantity more) {
    this.quantity = quantity.plus(more);
  }
}
