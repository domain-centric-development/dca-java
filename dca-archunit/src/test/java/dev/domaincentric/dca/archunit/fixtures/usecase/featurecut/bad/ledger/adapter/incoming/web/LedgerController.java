package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.application.booking.postentry.PostEntryInputPort;
import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.application.reporting.exportstatement.ExportStatementInputPort;
import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.application.reporting.showbalance.ShowBalanceInputPort;

/** One controller that changes state through the commands of two features. */
public final class LedgerController {
  private final PostEntryInputPort postEntry;
  private final ExportStatementInputPort exportStatement;
  private final ShowBalanceInputPort showBalance;

  public LedgerController(
      PostEntryInputPort postEntry,
      ExportStatementInputPort exportStatement,
      ShowBalanceInputPort showBalance) {
    this.postEntry = postEntry;
    this.exportStatement = exportStatement;
    this.showBalance = showBalance;
  }
}
