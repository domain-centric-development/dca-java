package dev.domaincentric.dca.archunit.fixtures.hexagonal.enumnames.bad.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.enumnames.bad.ledger.domain.model.AccountStatus;

public final class LedgerController {
  public String status(AccountStatus status) {
    return status.name();
  }

  public String label(AccountStatus status) {
    return status.toString();
  }
}
