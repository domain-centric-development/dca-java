package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Injectable
public final class ModelTypeInjectable {
  private int value;

  public ModelTypeInjectable() {}

  public void operation() {}
}
