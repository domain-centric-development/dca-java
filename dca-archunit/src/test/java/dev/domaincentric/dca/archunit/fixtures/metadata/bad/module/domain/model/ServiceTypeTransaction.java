package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Transaction
public final class ServiceTypeTransaction implements DomainService {
  private int value;

  public ServiceTypeTransaction() {}

  public void operation() {}
}
