package dev.domaincentric.dca.archunit.fixtures.contextmap.good.cart.domain.model;

import dev.domaincentric.dca.archunit.fixtures.contextmap.good.sharedkernel.domain.model.Money;

public final class Cart {
  private Money total = new Money(0);

  public Money total() {
    return total;
  }
}
