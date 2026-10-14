package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.UUID;

public final class Account extends BaseAggregateRoot<Account, AccountId> {
  private final AccountId id;

  private Account(AccountId id) {
    this.id = id;
  }

  public static Account open(AccountId id) {
    Account account = new Account(id);
    account.registerEvent(new AccountOpened(UUID.randomUUID(), Instant.now(), id));
    return account;
  }

  public static Account reconstitute(AccountId id) {
    return new Account(id);
  }

  @Override
  public AccountId id() {
    return id;
  }
}
