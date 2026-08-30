package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shiporder;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shared.CarrierPort;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// DCA-USE-013: @Transactional and calls a remote-capable port (CarrierPort) inside the transaction
@Service
@Transactional
public final class ShipOrderUseCase {
  private final OrderRepository orders;
  private final CarrierPort carrier;
  private final DomainEventPublisher events;

  public ShipOrderUseCase(
      OrderRepository orders, CarrierPort carrier, DomainEventPublisher events) {
    this.orders = orders;
    this.carrier = carrier;
    this.events = events;
  }

  public String execute(UUID orderId) {
    OrderId id = new OrderId(orderId);
    Order order = orders.findById(id).orElseThrow();
    String quote = carrier.quote(id);
    orders.save(order);
    events.publishAndClearEvents(order);
    return quote;
  }
}
