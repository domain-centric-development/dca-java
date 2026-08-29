package dev.domaincentric.dca.archunit.fixtures.contextmap.bad.external.payment;

public interface PaymentClient {
  String charge(long cents);
}
