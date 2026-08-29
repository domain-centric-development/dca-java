package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Specification;
import org.springframework.stereotype.Component;

@Component
public final class PaidOrderSpecification implements Specification<Order> {
  @Override
  public boolean isSatisfiedBy(Order candidate) {
    return true;
  }
}
