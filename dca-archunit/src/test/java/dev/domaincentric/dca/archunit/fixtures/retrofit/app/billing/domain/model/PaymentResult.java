package dev.domaincentric.dca.archunit.fixtures.retrofit.app.billing.domain.model;

/** A sealed outcome the domain returns. */
public sealed interface PaymentResult {
  record Paid() implements PaymentResult {}

  record Short(long missingCents) implements PaymentResult {}
}
