package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.infrastructure.config;

import org.springframework.transaction.PlatformTransactionManager;

/**
 * The composition root declares the transaction manager (role transactionManager); it draws no
 * boundary and is not a LAY-004 finding.
 */
public class TransactionConfig {
  public PlatformTransactionManager transactionManager() {
    return new PlatformTransactionManager() {};
  }
}
