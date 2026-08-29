package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import org.springframework.stereotype.Service;

@Service
public final class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public PlaceOrderUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  @Override
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    Order order = Order.place(new OrderId(command.orderId()));
    orders.save(order);
    events.publishAndClearEvents(order);
    return new PlaceOrderResult(order.id().value());
  }
}
