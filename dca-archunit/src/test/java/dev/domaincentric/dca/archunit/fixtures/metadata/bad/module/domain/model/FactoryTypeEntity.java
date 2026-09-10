package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@dev.domaincentric.dca.archunit.fixtures.metadata.annotations.Entity
public final class FactoryTypeEntity implements Factory {
  private int value;

  public FactoryTypeEntity() {}

  public void operation() {}
}
