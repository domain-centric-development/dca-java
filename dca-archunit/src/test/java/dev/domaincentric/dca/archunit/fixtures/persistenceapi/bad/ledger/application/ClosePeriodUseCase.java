package dev.domaincentric.dca.archunit.fixtures.persistenceapi.bad.ledger.application;

import dev.domaincentric.dca.archunit.fixtures.persistenceapi.bad.vendor.Session;

/** Uses a persistence library outside the default list: not reported unless the layout names it. */
public class ClosePeriodUseCase {

  private final Session session;

  public ClosePeriodUseCase(Session session) {
    this.session = session;
  }

  public void execute() {
    session.flush();
  }
}
