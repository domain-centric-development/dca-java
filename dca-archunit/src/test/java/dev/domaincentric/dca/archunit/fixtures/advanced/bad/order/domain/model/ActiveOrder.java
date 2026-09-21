package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Specification;
import org.springframework.stereotype.Component;

/**
 * DCA-ADV-018: the marker without the name suffix, carrying a container stereotype. The
 * specification role decides the owner, so this is reported here and not by DCA-ONI-003.
 */
@Component
public final class ActiveOrder implements Specification<Order> {
  @Override
  public boolean isSatisfiedBy(Order candidate) {
    return true;
  }
}
