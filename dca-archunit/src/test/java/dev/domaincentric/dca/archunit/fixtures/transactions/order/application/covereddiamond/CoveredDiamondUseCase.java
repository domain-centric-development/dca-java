package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.covereddiamond;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Valid: both routes from {@code execute} to {@code publish} draw a boundary. */
@Service
public final class CoveredDiamondUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;
  private final TransactionBoundary transactionBoundary;

  public CoveredDiamondUseCase(
      OrderRepository orders,
      DomainEventPublisher events,
      TransactionBoundary transactionBoundary) {
    this.orders = orders;
    this.events = events;
    this.transactionBoundary = transactionBoundary;
  }

  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    first(order);
    second(order);
    return order.id().value();
  }

  private void first(Order order) {
    transactionBoundary.inTransaction(
        () -> {
          orders.save(order);
          publish(order);
        });
  }

  private void second(Order order) {
    transactionBoundary.inTransaction(() -> publish(order));
  }

  private void publish(Order order) {
    events.publishAndClearEvents(order);
  }
}
