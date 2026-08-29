package dev.domaincentric.dca.archunit.fixtures.cycles.bad.beta.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.adapter.incoming.AlphaController;

public class BetaController {
  public String handle(AlphaController alpha) {
    return alpha.name();
  }

  public String name() {
    return "beta";
  }
}
