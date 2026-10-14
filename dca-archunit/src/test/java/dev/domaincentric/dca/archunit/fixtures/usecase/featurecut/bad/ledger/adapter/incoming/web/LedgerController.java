package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.application.booking.postentry.PostEntryInputPort;
import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.application.reporting.showbalance.ShowBalanceInputPort;

/** One controller for the use cases of two features. */
public final class LedgerController {
  private final PostEntryInputPort postEntry;
  private final ShowBalanceInputPort showBalance;

  public LedgerController(PostEntryInputPort postEntry, ShowBalanceInputPort showBalance) {
    this.postEntry = postEntry;
    this.showBalance = showBalance;
  }
}
