package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Order;

// DCA-USE-015: the base binds T to an aggregate, but no instance field uses T - nothing is exposed
public final class OrderCountResult extends UnusedParameterBase<Order> {
  public OrderCountResult(int count) {
    super(count);
  }
}
