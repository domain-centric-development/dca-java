package dev.domaincentric.dca.archunit.fixtures.contextmap.bad.cart.application.addtocart;

import dev.domaincentric.dca.archunit.fixtures.contextmap.bad.catalog.api.ProductInfo;

public final class CatalogLookup {
  public String name(ProductInfo info) {
    return info.name();
  }
}
