package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

public final class ServiceFieldMapping implements DomainService {
  @Mapping private int value;

  public ServiceFieldMapping() {}

  public void operation() {}
}
