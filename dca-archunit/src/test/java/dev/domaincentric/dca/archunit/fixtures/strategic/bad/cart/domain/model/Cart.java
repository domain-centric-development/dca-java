package dev.domaincentric.dca.archunit.fixtures.strategic.bad.cart.domain.model;

import dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.domain.model.Product;

public final class Cart {
  private Product lastAdded;

  public void add(Product product) {
    this.lastAdded = product;
  }
}
