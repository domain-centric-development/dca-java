package dev.domaincentric.dca.archunit.fixtures.operations.cycle.module.application.feature.target;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public interface TargetInputPort extends InputPort {
  void execute();
}
