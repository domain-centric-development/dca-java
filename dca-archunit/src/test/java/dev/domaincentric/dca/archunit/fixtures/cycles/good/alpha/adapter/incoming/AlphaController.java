package dev.domaincentric.dca.archunit.fixtures.cycles.good.alpha.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.cycles.good.beta.adapter.incoming.BetaController;

public class AlphaController {
  private final BetaController beta = new BetaController();

  public String handle() {
    return beta.handle();
  }
}
