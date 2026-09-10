package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Transaction
public final class FactoryTypeTransaction implements Factory {
  private int value;

  public FactoryTypeTransaction() {}

  public void operation() {}
}
