package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.OrderLineItem;

// DCA-USE-015: an entity, not an aggregate root, bound to the inherited type parameter
public final class LineItemResult extends GenericBase<OrderLineItem> {
  public LineItemResult(OrderLineItem item) {
    super(item, 1);
  }
}
