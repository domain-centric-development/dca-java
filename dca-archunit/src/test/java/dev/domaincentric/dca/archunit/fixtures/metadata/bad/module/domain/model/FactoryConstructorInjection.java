package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class FactoryConstructorInjection implements Factory {
  private int value;

  @Injection
  public FactoryConstructorInjection() {}

  public void operation() {}
}
