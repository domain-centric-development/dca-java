package dev.domaincentric.dca.archunit.fixtures.contextmap.good.cart.adapter.incoming.event;

import dev.domaincentric.dca.archunit.fixtures.contextmap.good.catalog.events.ProductPriceChangedEvent;

public final class ProductPriceChangedConsumer {
  public void on(ProductPriceChangedEvent event) {
    event.productId();
  }
}
