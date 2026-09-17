package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.sharedkernel.infrastructure;

import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;

/**
 * Shared-kernel plumbing collaborating with the transaction boundary (an outbox hooking into its
 * commit); it draws no boundary and is not a LAY-004 finding.
 */
public class OutboxHook {
  private final TransactionBoundary transactions;

  public OutboxHook(TransactionBoundary transactions) {
    this.transactions = transactions;
  }

  public TransactionBoundary boundary() {
    return transactions;
  }
}
