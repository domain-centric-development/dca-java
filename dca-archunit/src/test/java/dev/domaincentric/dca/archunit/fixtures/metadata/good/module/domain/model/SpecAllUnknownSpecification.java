package dev.domaincentric.dca.archunit.fixtures.metadata.good.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Unknown
public final class SpecAllUnknownSpecification {
  @Unknown private int value;

  @Unknown
  public SpecAllUnknownSpecification() {}

  @Unknown
  public void operation() {}
}
