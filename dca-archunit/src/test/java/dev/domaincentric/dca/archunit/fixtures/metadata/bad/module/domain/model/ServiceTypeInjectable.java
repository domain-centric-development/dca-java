package dev.domaincentric.dca.archunit.fixtures.metadata.bad.module.domain.model;

import dev.domaincentric.dca.archunit.fixtures.metadata.annotations.*;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.*;

@Injectable
public final class ServiceTypeInjectable implements DomainService {
  private int value;

  public ServiceTypeInjectable() {}

  public void operation() {}
}
