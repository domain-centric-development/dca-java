package dev.domaincentric.dca.archunit.fixtures.cycles.bad.beta.application.shared;

import dev.domaincentric.dca.archunit.fixtures.cycles.bad.alpha.application.shared.AlphaService;

public class BetaService {
  public String run(AlphaService alpha) {
    return alpha.name();
  }

  public String name() {
    return "beta";
  }
}
