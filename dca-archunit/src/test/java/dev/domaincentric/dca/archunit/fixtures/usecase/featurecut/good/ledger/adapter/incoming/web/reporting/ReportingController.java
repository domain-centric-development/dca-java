package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.adapter.incoming.web.reporting;

import dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.application.reporting.showbalance.ShowBalanceInputPort;

public final class ReportingController {
  private final ShowBalanceInputPort showBalance;

  public ReportingController(ShowBalanceInputPort showBalance) {
    this.showBalance = showBalance;
  }
}
