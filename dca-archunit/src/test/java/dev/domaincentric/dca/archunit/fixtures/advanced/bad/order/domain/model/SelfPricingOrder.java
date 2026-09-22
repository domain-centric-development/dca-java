package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model;

import dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.service.OrderTotalCalculator;
import dev.domaincentric.dca.archunit.fixtures.advanced.bad.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.math.BigDecimal;

/**
 * Violates DCA-ADV-019: the aggregate reaches for a domain service and constructs one itself, so
 * the decision that belongs to the application happens inside the model.
 */
public final class SelfPricingOrder extends BaseAggregateRoot<SelfPricingOrder, OrderId> {
  private final OrderId id;
  private final Money net;

  public SelfPricingOrder(OrderId id, Money net) {
    this.id = id;
    this.net = net;
  }

  @Override
  public OrderId id() {
    return id;
  }

  public Money total() {
    return new OrderTotalCalculator(new BigDecimal("0.19")).withTax(net);
  }
}
