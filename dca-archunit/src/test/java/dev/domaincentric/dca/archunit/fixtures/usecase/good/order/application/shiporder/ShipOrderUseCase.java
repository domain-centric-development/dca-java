package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shiporder;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared.CarrierPort;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import org.springframework.stereotype.Service;

// DCA-USE-012/013: remote call first, then an explicit TransactionBoundary boundary around save +
// publish
@Service
public final class ShipOrderUseCase implements ShipOrderInputPort {
  private final OrderRepository orders;
  private final CarrierPort carrier;
  private final DomainEventPublisher events;
  private final TransactionBoundary transactionBoundary;

  public ShipOrderUseCase(
      OrderRepository orders,
      CarrierPort carrier,
      DomainEventPublisher events,
      TransactionBoundary transactionBoundary) {
    this.orders = orders;
    this.carrier = carrier;
    this.events = events;
    this.transactionBoundary = transactionBoundary;
  }

  @Override
  public ShipOrderResult execute(ShipOrderCommand command) {
    OrderId id = new OrderId(command.orderId());
    String quote = carrier.quote(id);
    return transactionBoundary.inTransaction(
        () -> {
          Order order = orders.findById(id).orElseThrow();
          orders.save(order);
          events.publishAndClearEvents(order);
          return new ShipOrderResult(order.id().value(), quote);
        });
  }
}
