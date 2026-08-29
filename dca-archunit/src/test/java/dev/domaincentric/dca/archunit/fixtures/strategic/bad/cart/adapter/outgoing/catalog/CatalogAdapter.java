package dev.domaincentric.dca.archunit.fixtures.strategic.bad.cart.adapter.outgoing.catalog;

import dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.application.findproduct.FindProductUseCase;
import dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.domain.model.Product;

public final class CatalogAdapter {
  private final FindProductUseCase findProduct = new FindProductUseCase();

  public Product find(String name) {
    return findProduct.find(name);
  }
}
