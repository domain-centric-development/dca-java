package dev.domaincentric.dca.archunit.fixtures.persistenceapi.bad.ledger.application;

import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;

/**
 * Declares its transaction - that passes - and then draws one programmatically as well: the user
 * transaction is reported.
 */
@Transactional
public class SettlePeriodUseCase {

  private final UserTransaction transaction;

  public SettlePeriodUseCase(UserTransaction transaction) {
    this.transaction = transaction;
  }

  public void execute() {
    transaction.begin();
    transaction.commit();
  }
}
