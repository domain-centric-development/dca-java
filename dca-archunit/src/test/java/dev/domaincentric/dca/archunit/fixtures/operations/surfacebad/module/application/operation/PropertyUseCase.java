package dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application.operation;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class PropertyUseCase
    implements dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application
        .shared.Port {
  public String execute(String value) {
    return value;
  }

  public int getX() {
    return 1;
  }

  public void setX(int value) {}
}
