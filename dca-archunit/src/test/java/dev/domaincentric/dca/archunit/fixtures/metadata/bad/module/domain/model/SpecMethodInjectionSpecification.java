package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class SpecMethodInjectionSpecification {
  private int value;

  public SpecMethodInjectionSpecification() {}

  @Injection
  public void operation() {}
}
