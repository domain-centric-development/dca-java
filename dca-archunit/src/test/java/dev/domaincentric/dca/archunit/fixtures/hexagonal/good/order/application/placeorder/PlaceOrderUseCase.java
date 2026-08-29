package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.domain.model.OrderId;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.sharedkernel.domain.model.Money;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;

  public PlaceOrderUseCase(OrderRepository orders) {
    this.orders = orders;
  }

  @Override
  @Transactional
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    Order order = new Order(new OrderId(UUID.randomUUID().toString()), new Money(command.cents()));
    orders.save(order);
    return new PlaceOrderResult(order.id().value());
  }
}
