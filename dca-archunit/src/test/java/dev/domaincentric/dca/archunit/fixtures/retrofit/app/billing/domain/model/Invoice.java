package dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.retrofit.app.common.domain.model.Money;

public final class Invoice {
  private final Money total;

  public Invoice(Money total) {
    this.total = total;
  }

  public PaymentResult pay(Money amount) {
    return amount.cents() >= total.cents()
        ? new PaymentResult.Paid()
        : new PaymentResult.Short(total.cents() - amount.cents());
  }
}
