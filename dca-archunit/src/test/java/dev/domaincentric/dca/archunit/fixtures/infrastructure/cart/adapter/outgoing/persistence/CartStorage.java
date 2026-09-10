package dev.domaincentric.dca.archunit.fixtures.infrastructure.cart.adapter.outgoing.persistence;

import dev.domaincentric.dca.archunit.fixtures.infrastructure.cart.infrastructure.CartWiring;
import dev.domaincentric.dca.archunit.fixtures.infrastructure.sharedkernel.infrastructure.Lifecycle;

/** DCA-HEX-005 for CartWiring; the shared kernel's Lifecycle annotation is allowed. */
@Lifecycle
public final class CartStorage {
  private final dev.domaincentric.dca.archunit.fixtures.infrastructure.infrastructure.Wiring
      global = new dev.domaincentric.dca.archunit.fixtures.infrastructure.infrastructure.Wiring();
  private final CartWiring wiring = new CartWiring();

  public String describe() {
    return wiring.toString();
  }
}
