package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.good.ledger.application.closeaccount;

import dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.good.ledger.domain.model.AccountId;
import dev.domaincentric.dca.buildingblocks.application.UseCaseException;

@SuppressWarnings("serial") // a fixture: what the fields hold is the point, not serialisation
public final class AccountNotFoundException extends UseCaseException {
  private final AccountId accountId;

  public AccountNotFoundException(AccountId accountId) {
    super("account " + accountId.value() + " not found");
    this.accountId = accountId;
  }

  public AccountId accountId() {
    return accountId;
  }
}
