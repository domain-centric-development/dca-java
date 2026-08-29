package dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.application.shared;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.beta.application.shared.BetaService;

public class AlphaService {
  public String run(BetaService beta) {
    return beta.name();
  }

  public String name() {
    return "alpha";
  }
}
