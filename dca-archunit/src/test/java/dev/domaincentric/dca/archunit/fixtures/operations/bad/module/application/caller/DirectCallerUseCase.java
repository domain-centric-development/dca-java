package dev.domaincentric.dca.archunit.fixtures.operations.bad.module.application.caller;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class DirectCallerUseCase implements InputPort {
  private dev.domaincentric.dca.archunit.fixtures.operations.bad.module.application.target
          .TargetUseCase
      target;
}
