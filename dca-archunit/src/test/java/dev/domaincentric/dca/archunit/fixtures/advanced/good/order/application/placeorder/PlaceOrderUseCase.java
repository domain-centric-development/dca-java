package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.model.OrderFactory;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.service.OrderTotalCalculator;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.sharedkernel.domain.model.Money;

public final class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;
  private final OrderTotalCalculator calculator;
  private final OrderFactory factory = new OrderFactory();

  public PlaceOrderUseCase(OrderRepository orders, OrderTotalCalculator calculator) {
    this.orders = orders;
    this.calculator = calculator;
  }

  @Override
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    Order order = orders.save(factory.create(new Money(command.total())));
    return new PlaceOrderResult(order.id(), calculator.totalWithTax(order));
  }
}
