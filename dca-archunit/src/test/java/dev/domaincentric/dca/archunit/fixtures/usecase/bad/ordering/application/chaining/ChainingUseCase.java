package dev.domaincentric.dca.archunit.fixtures.usecase.bad.ordering.application.chaining;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;

public class ChainingUseCase implements InputPort {
  private OtherInputPort target;

  public void extra() {}
}

interface OtherInputPort extends InputPort {}
