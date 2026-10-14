package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;
import java.util.List;

/** Carries the id and the figures the rule compared. */
@SuppressWarnings("serial") // a fixture: what the fields hold is the point, not serialisation
public final class AccountOverdrawnException extends DomainException {
  private final AccountId accountId;
  private final Money balance;
  private final long requestedCents;
  private final List<AccountId> blockedBy;

  public AccountOverdrawnException(
      AccountId accountId, Money balance, long requestedCents, List<AccountId> blockedBy) {
    super("account " + accountId.value() + " overdrawn");
    this.accountId = accountId;
    this.balance = balance;
    this.requestedCents = requestedCents;
    this.blockedBy = List.copyOf(blockedBy);
  }

  public AccountId accountId() {
    return accountId;
  }

  public Money balance() {
    return balance;
  }

  public long requestedCents() {
    return requestedCents;
  }

  public List<AccountId> blockedBy() {
    return blockedBy;
  }
}
