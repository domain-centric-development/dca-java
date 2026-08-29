package dev.domaincentric.dca.archunit.fixtures.contextmap.bad.cart.domain.model;

import dev.domaincentric.dca.archunit.fixtures.contextmap.bad.external.payment.PaymentClient;

public final class Payment {
  private final PaymentClient client;

  public Payment(PaymentClient client) {
    this.client = client;
  }
}
