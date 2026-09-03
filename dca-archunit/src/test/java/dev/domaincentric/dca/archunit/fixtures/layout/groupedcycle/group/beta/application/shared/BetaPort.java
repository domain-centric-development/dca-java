package dev.domaincentric.dca.archunit.fixtures.layout.groupedcycle.group.beta.application.shared;

import dev.domaincentric.dca.archunit.fixtures.layout.groupedcycle.group.alpha.application.shared.AlphaPort;

/** The other half. */
public interface BetaPort {
  AlphaPort alpha();
}
