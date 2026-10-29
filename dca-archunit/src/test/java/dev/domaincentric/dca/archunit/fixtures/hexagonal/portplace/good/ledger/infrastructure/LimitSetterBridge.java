package dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.infrastructure;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.application.changelimit.ChangeLimitInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.portplace.good.ledger.application.shared.LimitSetter;

/** Hands the output port a second use case: wiring, not an adapter. */
public class LimitSetterBridge implements LimitSetter {
  private final ChangeLimitInputPort changeLimit;

  public LimitSetterBridge(ChangeLimitInputPort changeLimit) {
    this.changeLimit = changeLimit;
  }

  @Override
  public void setLimit(String account, long amount) {
    changeLimit.change(account, amount);
  }
}
