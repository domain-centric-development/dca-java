package dev.domaincentric.dca.archunit.fixtures.contextmap.nomodule.billing.adapter.outgoing.ledger;

import dev.domaincentric.dca.archunit.fixtures.contextmap.nomodule.ledger.api.LedgerApi;

public class LedgerClient {
  public String balance(final LedgerApi ledger) {
    return ledger.balance("main");
  }
}
