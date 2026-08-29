package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.infrastructure.config.InMemoryConfig;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.adapter.outgoing.InMemoryOrderRepository;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.model.OrderId;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.sharedkernel.domain.model.Money;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Violates HEX-002 (adapter dependency) and LAY-003 (infrastructure implementation).
@Service
public class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final InMemoryOrderRepository orders = new InMemoryOrderRepository();

  @Override
  @Transactional
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    Order order =
        new Order(
            new OrderId(UUID.randomUUID().toString()),
            new Money(command.cents() + InMemoryConfig.capacity()));
    orders.save(order);
    return new PlaceOrderResult(order.id().value());
  }
}
