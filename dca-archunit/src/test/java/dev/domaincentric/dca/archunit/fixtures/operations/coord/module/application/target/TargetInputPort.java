package dev.domaincentric.dca.archunit.fixtures.operations.coord.module.application.target;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public interface TargetInputPort extends InputPort {
  void execute();
}
