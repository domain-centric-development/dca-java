package dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.ordering.placeorder;

import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import org.springframework.stereotype.Service;

@Service
public final class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;
  private final FraudCheckPort fraudCheck;
  private final DomainEventPublisher events;
  private final TransactionBoundary transactionBoundary;

  public PlaceOrderUseCase(
      OrderRepository orders,
      FraudCheckPort fraudCheck,
      DomainEventPublisher events,
      TransactionBoundary transactionBoundary) {
    this.orders = orders;
    this.fraudCheck = fraudCheck;
    this.events = events;
    this.transactionBoundary = transactionBoundary;
  }

  @Override
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    OrderId id = new OrderId(command.orderId());
    boolean cleared = fraudCheck.clears(id);
    return transactionBoundary.inTransaction(
        () -> {
          Order order = Order.place(id);
          orders.save(order);
          events.publishAndClearEvents(order);
          return new PlaceOrderResult(order.id().value(), cleared);
        });
  }
}
