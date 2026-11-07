package dev.domaincentric.dca.archunit.fixtures.tactical.primitives.bad.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.List;

/**
 * An aggregate holding a count, a moment and a list of strings bare, and taking flag arguments - a
 * boolean parameter and a public boolean field. Its yes/no fact {@code closed}, a private boolean
 * field, passes.
 */
public class Account extends BaseAggregateRoot<Account, AccountId> {
  private final AccountId id;
  private boolean closed;
  public boolean frozen;
  private int overdraftCount;
  private Instant openedAt;
  private List<String> tags;
  private java.util.UUID ownerId;

  Account(AccountId id) {
    this.id = id;
  }

  @Override
  public AccountId id() {
    return id;
  }

  public void rename(String holder) {}

  public void markClosed(boolean closed) {
    this.closed = closed;
  }

  public boolean isClosed() {
    return closed;
  }

  @Override
  public String toString() {
    return id.value();
  }
}
