package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

public final class Account extends BaseAggregateRoot<Account, AccountId> {
  private final AccountId id;

  private Account(AccountId id) {
    this.id = id;
  }

  public static Account reconstitute(AccountId id) {
    return new Account(id);
  }

  @Override
  public AccountId id() {
    return id;
  }
}
