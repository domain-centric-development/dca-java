package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;
import java.util.List;

// DCA-USE-015: two levels of inheritance - T = List<U>, U = Order - resolve to List<Order>
public final class BatchedOrdersResult extends Intermediate<Order> {
  public BatchedOrdersResult(List<Order> orders) {
    super(orders, orders.size());
  }
}
