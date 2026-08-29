package dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.beta.adapter.outgoing.BetaGateway;

public class AlphaGateway {
  public String call(BetaGateway beta) {
    return beta.name();
  }

  public String name() {
    return "alpha";
  }
}
