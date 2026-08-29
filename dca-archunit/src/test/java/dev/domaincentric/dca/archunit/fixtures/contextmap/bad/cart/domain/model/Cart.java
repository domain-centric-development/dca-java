package dev.domaincentric.dca.archunit.fixtures.contextmap.bad.cart.domain.model;

import dev.domaincentric.dca.archunit.fixtures.contextmap.bad.catalog.events.ProductPriceChangedEvent;

public final class Cart {
  private ProductPriceChangedEvent lastPriceChange;

  public void on(ProductPriceChangedEvent event) {
    this.lastPriceChange = event;
  }
}
