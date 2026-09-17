package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.infrastructure.config;

import org.springframework.transaction.support.TransactionTemplate;

/**
 * The composition root wires the transaction manager; it draws no boundary and is not a LAY-004
 * finding.
 */
public class TransactionConfig {
  public TransactionTemplate transactions() {
    return new TransactionTemplate();
  }
}
