package dev.domaincentric.dca.archunit.fixtures.strategic.good.cart.adapter.incoming.event.acl;

import dev.domaincentric.dca.archunit.fixtures.strategic.good.catalog.events.ProductAddedEvent;

public final class ProductEventTranslator {
  public String translate(ProductAddedEvent event) {
    return event.productId();
  }
}
