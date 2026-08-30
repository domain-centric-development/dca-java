package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.infrastructure.config.InMemoryConfig;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.adapter.outgoing.InMemoryOrderRepository;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderUseCase;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

// Violates HEX-003 (repository access), HEX-004 (infrastructure implementation), HEX-006
// (incoming -> outgoing adapter), HEX-011 (the use case class instead of its input port) and
// LAY-004 (@Transactional on an incoming adapter).
@RestController
@Transactional
public class OrderResource {
  private final PlaceOrderInputPort placeOrder;
  private final InMemoryOrderRepository orders = new InMemoryOrderRepository();
  private final PlaceOrderUseCase placeOrderImplementation = null;

  public OrderResource(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }

  public String place(long cents) {
    orders.deleteById(null);
    return placeOrder.execute(new PlaceOrderCommand(cents + InMemoryConfig.capacity())).orderId();
  }
}
