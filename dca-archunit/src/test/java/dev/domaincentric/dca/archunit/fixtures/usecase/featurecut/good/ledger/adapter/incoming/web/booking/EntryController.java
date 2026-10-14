package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.adapter.incoming.web.booking;

import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.application.booking.postentry.PostEntryInputPort;
import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.application.reporting.showbalance.ShowBalanceInputPort;

/** Changes state through one feature and reads a neighbouring feature's query. */
public final class EntryController {
  private final PostEntryInputPort postEntry;
  private final ShowBalanceInputPort showBalance;

  public EntryController(PostEntryInputPort postEntry, ShowBalanceInputPort showBalance) {
    this.postEntry = postEntry;
    this.showBalance = showBalance;
  }
}
