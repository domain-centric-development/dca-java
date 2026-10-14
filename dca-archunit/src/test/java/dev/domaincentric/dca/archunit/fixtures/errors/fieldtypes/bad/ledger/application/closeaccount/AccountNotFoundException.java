package dev.domaincentric.dca.archunit.fixtures.errors.fieldtypes.bad.ledger.application.closeaccount;

import dev.domaincentric.dca.buildingblocks.application.UseCaseException;

/** Carries the caller's raw input instead of the typed id. */
@SuppressWarnings("serial") // a fixture: what the fields hold is the point, not serialisation
public final class AccountNotFoundException extends UseCaseException {
  private final String rawAccountId;

  public AccountNotFoundException(String rawAccountId) {
    super("account not found");
    this.rawAccountId = rawAccountId;
  }

  public String rawAccountId() {
    return rawAccountId;
  }
}
