package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

public class Account extends BaseAggregateRoot<Account, AccountId> {
  private final AccountId id;
  private AccountStatus status;
  private Address address;
  private boolean frozen;

  Account(AccountId id) {
    this.id = id;
    this.status = AccountStatus.OPEN;
  }

  /** Rebuilds a stored account; the stored yes/no fact comes back as the boolean it was. */
  static Account reconstitute(AccountId id, AccountStatus status, Address address, boolean frozen) {
    Account account = new Account(id);
    account.status = status;
    account.address = address;
    account.frozen = frozen;
    return account;
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

  public void freeze() {
    this.frozen = true;
  }

  public boolean isFrozen() {
    return frozen;
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
