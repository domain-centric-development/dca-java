package dev.domaincentric.dca.archunit.fixtures.strategic.bad.cart.application.addtocart;

import dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.domain.model.Product;

public final class AddToCartUseCase {
  public Product execute(String name) {
    return new Product(name);
  }
}
