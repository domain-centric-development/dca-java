package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.service;

import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;

// Violates LAY-004: a domain class depending on the TransactionBoundary port - transactions are an
// application-layer concern, the domain knows nothing of them.
public class TransactionalPricing {
  private final TransactionBoundary transactions;

  public TransactionalPricing(TransactionBoundary transactions) {
    this.transactions = transactions;
  }

  public long tax(long cents) {
    return transactions.inTransaction(() -> cents / 5);
  }
}
