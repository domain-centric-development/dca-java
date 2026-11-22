package dev.domaincentric.dca.archunit.fixtures.usecase.sharedport.bad.alpha.application.install;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

/** Declared in one use case, used by two: belongs in application.shared. */
public interface Source extends OutputPort {
  String read();
}
