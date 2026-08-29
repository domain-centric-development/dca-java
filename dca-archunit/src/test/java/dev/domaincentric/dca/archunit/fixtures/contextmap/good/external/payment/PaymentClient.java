package dev.domaincentric.dca.archunit.fixtures.contextmap.good.external.payment;

/** Stands in for a vendor SDK of an external system. */
public interface PaymentClient {
  String charge(long cents);
}
