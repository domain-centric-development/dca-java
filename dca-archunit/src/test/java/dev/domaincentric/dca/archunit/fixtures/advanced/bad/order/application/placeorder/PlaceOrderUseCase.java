package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model.OrderFactory;
import dev.domaincentric.dca.archunit.fixtures.advanced.bad.sharedkernel.domain.model.Money;

public final class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;
  private final OrderFactory factory = new OrderFactory();

  public PlaceOrderUseCase(OrderRepository orders) {
    this.orders = orders;
  }

  @Override
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    Order order = orders.save(factory.create(new Money(command.total())));
    return new PlaceOrderResult(order.id());
  }
}
