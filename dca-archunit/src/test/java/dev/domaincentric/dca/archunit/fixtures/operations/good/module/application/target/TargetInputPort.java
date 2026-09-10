package dev.domaincentric.dca.archunit.fixtures.operations.good.module.application.target;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public interface TargetInputPort extends InputPort {
  void execute();
}
