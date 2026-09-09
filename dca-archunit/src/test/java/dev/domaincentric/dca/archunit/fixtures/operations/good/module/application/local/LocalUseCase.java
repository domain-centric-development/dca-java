package dev.domaincentric.dca.archunit.fixtures.operations.good.module.application.local;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class LocalUseCase implements UseCase<String, String> {
  private dev.domaincentric.dca.archunit.fixtures.operations.good.module.application.shared
          .Collaborator
      helper;

  public String execute(String value) {
    return helper.calculate(value);
  }
}
