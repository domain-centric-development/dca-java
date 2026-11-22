package dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.good.alpha.application.own;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

/** Used by its own use case only, declared beside it. */
public interface OwnStore extends OutputPort {
  String read();
}
