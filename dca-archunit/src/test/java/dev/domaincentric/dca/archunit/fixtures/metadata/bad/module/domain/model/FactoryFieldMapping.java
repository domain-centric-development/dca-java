package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class FactoryFieldMapping implements Factory {
  @Mapping private int value;

  public FactoryFieldMapping() {}

  public void operation() {}
}
