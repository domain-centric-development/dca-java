package dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application.operation;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class UnrelatedUseCase
    implements dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application
            .shared.Port,
        dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application.shared
            .Unrelated {
  public String execute(String value) {
    return value;
  }

  public void audit() {}
}
