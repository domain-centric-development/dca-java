package dev.domaincentric.dca.archunit.fixtures.operations.cycle.module.application.feature.target;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class TargetUseCase implements TargetInputPort {
  public void execute() {}

  private dev.domaincentric.dca.archunit.fixtures.operations.cycle.module.application.feature
          .coordinator.CoordinatorUseCase
      coordinator;
}
