package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.domain.model.OrderId;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.domain.service.PricingPolicy;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.sharedkernel.domain.model.Money;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;
  private final PricingPolicy pricing;

  public PlaceOrderUseCase(OrderRepository orders, PricingPolicy pricing) {
    this.orders = orders;
    this.pricing = pricing;
  }

  @Override
  @Transactional
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    Order order =
        new Order(
            new OrderId(UUID.randomUUID().toString()), pricing.withTax(new Money(command.cents())));
    orders.save(order);
    return new PlaceOrderResult(order.id().value());
  }
}
