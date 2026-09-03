package dev.domaincentric.dca.archunit.fixtures.layout.isolation.peer.adapter.outgoing.catalog;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.api.CatalogService;

/** Allowed: an outgoing adapter depending on another module's published api package. */
public class CatalogClient {
  private final CatalogService catalog;

  public CatalogClient(final CatalogService catalog) {
    this.catalog = catalog;
  }

  public String describe(final String sku) {
    return catalog.describe(sku);
  }
}
