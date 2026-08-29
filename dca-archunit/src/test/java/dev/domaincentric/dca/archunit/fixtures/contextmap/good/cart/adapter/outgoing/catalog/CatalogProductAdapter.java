package dev.domaincentric.dca.archunit.fixtures.contextmap.good.cart.adapter.outgoing.catalog;

import dev.domaincentric.dca.archunit.fixtures.contextmap.good.catalog.api.CatalogApi;
import dev.domaincentric.dca.archunit.fixtures.contextmap.good.catalog.api.ProductInfo;

public final class CatalogProductAdapter {
  private final CatalogApi catalogApi;

  public CatalogProductAdapter(CatalogApi catalogApi) {
    this.catalogApi = catalogApi;
  }

  public String lineItemName(String productId) {
    ProductInfo info = catalogApi.product(productId);
    return info.name();
  }
}
