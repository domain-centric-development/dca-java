package dev.domaincentric.dca.archunit.fixtures.cycles.good.alpha.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.cycles.good.beta.adapter.outgoing.BetaGateway;

public class AlphaGateway {
  private final BetaGateway beta = new BetaGateway();

  public String call() {
    return beta.call();
  }
}
