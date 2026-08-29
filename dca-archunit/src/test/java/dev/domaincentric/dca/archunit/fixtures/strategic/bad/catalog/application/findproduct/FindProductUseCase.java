package dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.application.findproduct;

import dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.domain.model.Product;

public final class FindProductUseCase {
  public Product find(String name) {
    return new Product(name);
  }
}
