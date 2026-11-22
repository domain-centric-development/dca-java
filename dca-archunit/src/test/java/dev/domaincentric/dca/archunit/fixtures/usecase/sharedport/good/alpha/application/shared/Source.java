package dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.good.alpha.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

/** Used by two use cases, declared in application.shared. */
public interface Source extends OutputPort {
  String read();
}
