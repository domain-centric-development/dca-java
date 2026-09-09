package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class SpecConstructorInjectionSpecification {
  private int value;

  @Injection
  public SpecConstructorInjectionSpecification() {}

  public void operation() {}
}
