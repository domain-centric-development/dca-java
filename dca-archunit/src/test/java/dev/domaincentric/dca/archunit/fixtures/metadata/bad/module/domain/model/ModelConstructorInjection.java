package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class ModelConstructorInjection {
  private int value;

  @Injection
  public ModelConstructorInjection() {}

  public void operation() {}
}
