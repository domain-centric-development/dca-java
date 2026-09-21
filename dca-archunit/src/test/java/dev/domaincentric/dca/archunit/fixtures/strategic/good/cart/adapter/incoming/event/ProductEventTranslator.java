package dev.domaincentric.dca.archunit.fixtures.strategic.good.cart.adapter.incoming.event;

import dev.domaincentric.dca.archunit.fixtures.strategic.good.catalog.events.ProductAddedEvent;

/** An anti-corruption layer lives in the incoming adapter; no acl segment is required. */
public final class ProductEventTranslator {
  public String translate(ProductAddedEvent event) {
    return event.productId();
  }
}
