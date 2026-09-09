package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class FactoryMethodListener implements Factory {
  private int value;

  public FactoryMethodListener() {}

  @Listener
  public void operation() {}
}
