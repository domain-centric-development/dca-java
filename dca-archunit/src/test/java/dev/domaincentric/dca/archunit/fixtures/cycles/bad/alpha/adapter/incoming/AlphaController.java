package dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.beta.adapter.incoming.BetaController;

public class AlphaController {
  public String handle(BetaController beta) {
    return beta.name();
  }

  public String name() {
    return "alpha";
  }
}
