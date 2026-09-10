package dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application.operation;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class InheritedUseCase
    extends dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application.shared
        .Base
    implements dev.domaincentric.dca.archunit.fixtures.operations.surfacebad.module.application
        .shared.Port {
  public String execute(String value) {
    return value;
  }
}
