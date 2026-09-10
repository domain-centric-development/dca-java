package dev.domaincentric.dca.archunit.fixtures.operations.bad.module.application.target;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public interface TargetInputPort extends InputPort {
  void execute();
}
