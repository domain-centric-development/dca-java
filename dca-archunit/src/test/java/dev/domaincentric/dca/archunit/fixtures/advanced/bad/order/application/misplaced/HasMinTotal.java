package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.application.misplaced;

import dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model.Order;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Specification;

/**
 * DCA-ADV-017: the marker without the name suffix — the shape both reference samples use. Selected
 * through the specification role, not the name.
 */
public final class HasMinTotal implements Specification<Order> {
  @Override
  public boolean isSatisfiedBy(Order candidate) {
    return true;
  }
}
