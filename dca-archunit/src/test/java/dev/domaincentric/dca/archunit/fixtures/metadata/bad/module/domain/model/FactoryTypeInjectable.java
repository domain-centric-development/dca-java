package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Injectable
public final class FactoryTypeInjectable implements Factory {
  private int value;

  public FactoryTypeInjectable() {}

  public void operation() {}
}
