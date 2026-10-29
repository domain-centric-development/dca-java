package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.List;

/** An aggregate holding a flag, a count, a moment and a list of strings bare. */
public class Account extends BaseAggregateRoot<Account, AccountId> {
  private final AccountId id;
  private boolean closed;
  private int overdraftCount;
  private Instant openedAt;
  private List<String> tags;

  Account(AccountId id) {
    this.id = id;
  }

  @Override
  public AccountId id() {
    return id;
  }

  public void rename(String holder) {}

  public boolean isClosed() {
    return closed;
  }

  @Override
  public String toString() {
    return id.value();
  }
}
