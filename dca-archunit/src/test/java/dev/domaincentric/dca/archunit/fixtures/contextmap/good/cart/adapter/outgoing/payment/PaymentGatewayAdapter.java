package dev.domaincentric.dca.archunit.fixtures.contextmap.good.cart.adapter.outgoing.payment;

import dev.domaincentric.dca.archunit.fixtures.contextmap.good.external.payment.PaymentClient;

public final class PaymentGatewayAdapter {
  private final PaymentClient client;

  public PaymentGatewayAdapter(PaymentClient client) {
    this.client = client;
  }

  public String pay(long cents) {
    return client.charge(cents);
  }
}
