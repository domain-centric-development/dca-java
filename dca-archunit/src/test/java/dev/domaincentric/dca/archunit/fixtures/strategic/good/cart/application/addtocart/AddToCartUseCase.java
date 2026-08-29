package dev.domaincentric.dca.archunit.fixtures.strategic.good.cart.application.addtocart;

import dev.domaincentric.dca.archunit.fixtures.strategic.good.cart.domain.model.Cart;

public final class AddToCartUseCase {
  public Cart execute() {
    return new Cart();
  }
}
