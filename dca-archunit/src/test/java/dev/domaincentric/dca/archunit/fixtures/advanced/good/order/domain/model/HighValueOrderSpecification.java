package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Specification;
import java.math.BigDecimal;

public final class HighValueOrderSpecification implements Specification<Order> {
  private final BigDecimal threshold;

  public HighValueOrderSpecification(BigDecimal threshold) {
    this.threshold = threshold;
  }

  @Override
  public boolean isSatisfiedBy(Order candidate) {
    return candidate.total().amount().compareTo(threshold) >= 0;
  }
}
