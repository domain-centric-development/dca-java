package dev.domaincentric.dca.archunit.fixtures.layout.isolation.peer.adapter.incoming.event;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.domain.model.Product;

/**
 * Exempt from DCA-HEX-007: an event consumer - the configured event-consumer sub-package of the
 * incoming adapters - may depend on another module. Renaming that segment on the layout withdraws
 * the exemption.
 */
public class CatalogChangedConsumer {
  public String on(final Product product) {
    return product.sku();
  }
}
