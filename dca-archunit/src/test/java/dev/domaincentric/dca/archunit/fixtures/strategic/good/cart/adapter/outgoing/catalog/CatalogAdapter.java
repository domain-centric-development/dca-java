package dev.domaincentric.dca.archunit.fixtures.strategic.good.cart.adapter.outgoing.catalog;

import dev.domaincentric.dca.archunit.fixtures.strategic.good.catalog.api.CatalogApi;

public final class CatalogAdapter {
  private final CatalogApi catalogApi;

  public CatalogAdapter(CatalogApi catalogApi) {
    this.catalogApi = catalogApi;
  }

  public String nameOf(String productId) {
    return catalogApi.productName(productId);
  }
}
