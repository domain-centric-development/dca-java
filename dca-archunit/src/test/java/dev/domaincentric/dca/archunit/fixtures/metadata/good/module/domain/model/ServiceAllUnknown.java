package dev.domaincentric.dca.archunit.fixtures.metadata.good.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Unknown
public final class ServiceAllUnknown implements DomainService {
  @Unknown private int value;

  @Unknown
  public ServiceAllUnknown() {}

  @Unknown
  public void operation() {}
}
