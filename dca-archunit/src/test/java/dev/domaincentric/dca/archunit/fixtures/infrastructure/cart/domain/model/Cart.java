package dev.domaincentric.dca.archunit.fixtures.infrastructure.cart.domain.model;

import dev.domaincentric.dca.archunit.fixtures.infrastructure.cart.infrastructure.CartWiring;

/** DCA-LAY-002: the domain depends on the module's infrastructure. */
public final class Cart {
  private final CartWiring wiring = new CartWiring();

  public String describe() {
    return wiring.toString();
  }
}
