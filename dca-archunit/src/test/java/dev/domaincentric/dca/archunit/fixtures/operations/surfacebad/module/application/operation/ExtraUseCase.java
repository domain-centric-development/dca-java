package dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application.operation;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class ExtraUseCase
    implements dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application
        .shared.Port {
  public String execute(String value) {
    return value;
  }

  public void recalculate() {}
}
