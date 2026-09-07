package dev.domaincentric.dca.archunit.fixtures.infrastructure.cart.application.getcart;

import dev.domaincentric.dca.archunit.fixtures.infrastructure.cart.infrastructure.CartWiring;
import dev.domaincentric.dca.archunit.fixtures.infrastructure.infrastructure.Wiring;
import dev.domaincentric.dca.archunit.fixtures.infrastructure.infrastructurex.NotInfrastructure;

/** DCA-LAY-003: depends on the global and on the module's infrastructure implementation. */
public final class GetCartUseCase {
  private final Wiring wiring = new Wiring();
  private final CartWiring cartWiring = new CartWiring();
  private final NotInfrastructure fine = new NotInfrastructure();

  public Object execute() {
    return wiring.toString() + cartWiring + fine;
  }
}
