package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class ModelMethodListener {
  private int value;

  public ModelMethodListener() {}

  @Listener
  public void operation() {}
}
