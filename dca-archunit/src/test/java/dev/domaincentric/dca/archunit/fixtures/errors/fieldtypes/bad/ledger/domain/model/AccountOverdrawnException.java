package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;

/** Carries what a page shows and the aggregate itself. */
@SuppressWarnings("serial") // a fixture: what the fields hold is the point, not serialisation
public final class AccountOverdrawnException extends DomainException {
  private final String accountName;
  private final Account account;

  public AccountOverdrawnException(String accountName, Account account) {
    super("account overdrawn");
    this.accountName = accountName;
    this.account = account;
  }

  public String accountName() {
    return accountName;
  }

  public Account account() {
    return account;
  }
}
