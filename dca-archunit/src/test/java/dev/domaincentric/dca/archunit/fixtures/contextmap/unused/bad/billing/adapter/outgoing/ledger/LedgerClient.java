package dev.domaincentric.dca.archunit.fixtures.contextmap.unused.bad.billing.adapter.outgoing.ledger;

import dev.domaincentric.dca.archunit.fixtures.contextmap.unused.bad.ledger.api.LedgerApi;

public final class LedgerClient {
  private final LedgerApi ledger;

  public LedgerClient(LedgerApi ledger) {
    this.ledger = ledger;
  }

  public long balanceOf(String account) {
    return ledger.balanceOf(account);
  }
}
