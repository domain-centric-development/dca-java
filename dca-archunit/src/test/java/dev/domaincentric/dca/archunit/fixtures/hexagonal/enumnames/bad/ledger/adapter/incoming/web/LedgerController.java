package dev.domaincentric.dca.archunit.fixtures.hexagonal.enumnames.bad.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.enumnames.bad.ledger.domain.model.AccountStatus;

public final class LedgerController {
  public String status(AccountStatus status) {
    return status.name();
  }

  public String label(AccountStatus status) {
    return status.toString();
  }

  /**
   * Concatenation of the value itself: the compiler passes it through String.valueOf(Object), so
   * the enum's type is gone from the call and the rule does not see it (documented in its checks
   * text).
   */
  public String concatenated(AccountStatus status) {
    return "status=" + status;
  }

  /** Concatenation of the member name, called explicitly. */
  public String concatenatedName(AccountStatus status) {
    return "status=" + status.name();
  }
}
