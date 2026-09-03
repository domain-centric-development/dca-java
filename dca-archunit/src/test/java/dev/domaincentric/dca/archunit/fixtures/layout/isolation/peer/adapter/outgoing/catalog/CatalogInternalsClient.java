package dev.domaincentric.dca.archunit.fixtures.layout.isolation.peer.adapter.outgoing.catalog;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.domain.model.Product;

/** Forbidden: an outgoing adapter depending on another module's domain model (DCA-STR-006). */
public class CatalogInternalsClient {
  public String describe(final Product product) {
    return product.sku();
  }
}
