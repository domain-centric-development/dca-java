package dev.domaincentric.dca.archunit.fixtures.cycles.bad.beta.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.adapter.outgoing.AlphaGateway;

public class BetaGateway {
  public String call(AlphaGateway alpha) {
    return alpha.name();
  }

  public String name() {
    return "beta";
  }
}
