package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

public class Account extends BaseAggregateRoot<Account, AccountId> {
  private final AccountId id;
  private AccountStatus status;
  private Address address;

  Account(AccountId id) {
    this.id = id;
    this.status = AccountStatus.OPEN;
  }

  @Override
  public AccountId id() {
    return id;
  }

  public AccountStatus status() {
    return status;
  }

  public void moveTo(Address address) {
    this.address = address;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof Account account && account.id.equals(id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
