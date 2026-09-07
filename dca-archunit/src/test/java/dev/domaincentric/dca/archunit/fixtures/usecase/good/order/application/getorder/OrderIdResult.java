package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.OrderId;

// DCA-USE-015: the inherited type parameter bound to an Id is a value
public final class OrderIdResult extends GenericBase<OrderId> {
  public OrderIdResult(OrderId id) {
    super(id);
  }
}
