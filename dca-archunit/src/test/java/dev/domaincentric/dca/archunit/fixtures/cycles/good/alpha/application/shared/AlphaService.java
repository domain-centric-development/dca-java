package dev.domaincentric.dca.archunit.fixtures.cycles.good.alpha.application.shared;

import dev.domaincentric.dca.archunit.fixtures.cycles.good.beta.application.shared.BetaService;

public class AlphaService {
  private final BetaService beta = new BetaService();

  public String run() {
    return beta.run();
  }
}
