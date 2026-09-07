package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;

// DCA-USE-015: the aggregate arrives through the inherited field 'T value', with T bound to Order
public final class GenericOrderResult extends GenericBase<Order> {
  public GenericOrderResult(Order order) {
    super(order, 1);
  }
}
