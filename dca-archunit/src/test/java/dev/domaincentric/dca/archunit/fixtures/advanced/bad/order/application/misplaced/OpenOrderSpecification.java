package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.application.misplaced;

import dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model.Order;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Specification;

public final class OpenOrderSpecification implements Specification<Order> {
  @Override
  public boolean isSatisfiedBy(Order candidate) {
    return true;
  }
}
