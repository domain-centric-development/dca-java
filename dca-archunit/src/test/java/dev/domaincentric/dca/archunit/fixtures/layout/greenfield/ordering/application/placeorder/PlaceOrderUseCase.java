package dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.domain.model.Order;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public PlaceOrderUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  @Override
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    Order order = Order.place();
    orders.save(order);
    events.publishAndClearEvents(order);
    return PlaceOrderResult.from(order);
  }
}
