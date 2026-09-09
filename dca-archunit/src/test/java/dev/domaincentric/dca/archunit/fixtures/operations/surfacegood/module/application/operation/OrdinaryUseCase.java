package dev.domaincentric.dca.archunit.fixtures.operations.surfacegood.module.application.operation;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class OrdinaryUseCase
    implements dev.domaincentric.dca.archunit.fixtures.operations.surfacegood.module.application
        .shared.Port {
  public OrdinaryUseCase() {}

  public String execute(String value) {
    return value;
  }
}
