package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.genericport;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;

/**
 * DCA-USE-009: saves without publishing, so it is reported — but exactly once. The compiler's
 * bridge method {@code execute(Object)} must not count as a second entry point into {@code
 * execute(PlaceOrderCommand)}.
 */
public final class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;

  public PlaceOrderUseCase(OrderRepository orders) {
    this.orders = orders;
  }

  @Override
  public PlaceOrderResult execute(PlaceOrderCommand input) {
    Order order = new Order(new OrderId(input.orderId()));
    orders.save(order);
    return new PlaceOrderResult(order.id().value());
  }
}
