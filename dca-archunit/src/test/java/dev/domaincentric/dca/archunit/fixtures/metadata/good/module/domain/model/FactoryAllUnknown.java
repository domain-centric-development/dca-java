package dev.domaincentric.dca.archunit.fixtures.metadata.good.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Unknown
public final class FactoryAllUnknown implements Factory {
  @Unknown private int value;

  @Unknown
  public FactoryAllUnknown() {}

  @Unknown
  public void operation() {}
}
