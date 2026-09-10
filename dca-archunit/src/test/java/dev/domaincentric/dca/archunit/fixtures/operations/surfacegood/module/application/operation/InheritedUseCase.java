package dev.domaincentric.dca.archunit.fixtures.operations.surfacegood.module.application.operation;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class InheritedUseCase
    extends dev.domaincentric.dca.archunit.fixtures.operations.surfacegood.module.application.shared
            .Base<
        String> {
  public String toString() {
    return "inherited";
  }
}
