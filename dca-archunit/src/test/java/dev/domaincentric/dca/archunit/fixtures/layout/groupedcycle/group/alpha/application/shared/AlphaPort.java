package dev.domaincentric.dca.archunit.fixtures.layout.groupedcycle.group.alpha.application.shared;

import dev.domaincentric.dca.archunit.fixtures.layout.groupedcycle.group.beta.application.shared.BetaPort;

/** Half of a cycle between two contexts that both sit two segments below the base package. */
public interface AlphaPort {
  BetaPort beta();
}
