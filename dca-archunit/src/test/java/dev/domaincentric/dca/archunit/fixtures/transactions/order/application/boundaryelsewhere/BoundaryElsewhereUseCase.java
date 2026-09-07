package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.boundaryelsewhere;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * DCA-USE-012: {@code execute} draws a boundary around the save only; {@code notifyLater} publishes
 * in a method that never reaches a boundary.
 */
@Service
public final class BoundaryElsewhereUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;
  private final TransactionBoundary transactionBoundary;

  public BoundaryElsewhereUseCase(
      OrderRepository orders,
      DomainEventPublisher events,
      TransactionBoundary transactionBoundary) {
    this.orders = orders;
    this.events = events;
    this.transactionBoundary = transactionBoundary;
  }

  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    transactionBoundary.inTransaction(() -> orders.save(order));
    return order.id().value();
  }

  public void notifyLater(UUID orderId) {
    Order order = orders.findById(new OrderId(orderId)).orElseThrow();
    events.publishAndClearEvents(order);
  }
}
