package dev.domaincentric.dca.archunit.fixtures.operations.good.module.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.*;

public final class Collaborator {
  public String calculate(String value) {
    return value;
  }
}
