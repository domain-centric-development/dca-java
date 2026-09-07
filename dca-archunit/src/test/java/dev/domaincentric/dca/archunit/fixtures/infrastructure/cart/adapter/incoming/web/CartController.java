package dev.domaincentric.dca.archunit.fixtures.infrastructure.cart.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.infrastructure.infrastructure.Wiring;

/** DCA-HEX-004. */
public final class CartController {
  private final Wiring wiring = new Wiring();

  public String show() {
    return wiring.toString();
  }
}
