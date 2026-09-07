package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.mixeddiamond;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * DCA-USE-012: {@code execute} reaches {@code publish} on two routes - through {@code wrapped},
 * which draws a boundary, and through {@code plain}, which does not. The boundary on one route does
 * not cover the other: {@code execute -> plain -> publish} publishes outside any transaction.
 */
@Service
public final class MixedDiamondUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;
  private final TransactionBoundary transactionBoundary;

  public MixedDiamondUseCase(
      OrderRepository orders,
      DomainEventPublisher events,
      TransactionBoundary transactionBoundary) {
    this.orders = orders;
    this.events = events;
    this.transactionBoundary = transactionBoundary;
  }

  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    wrapped(order);
    plain(order);
    return order.id().value();
  }

  private void wrapped(Order order) {
    transactionBoundary.inTransaction(
        () -> {
          orders.save(order);
          publish(order);
        });
  }

  private void plain(Order order) {
    publish(order);
  }

  private void publish(Order order) {
    events.publishAndClearEvents(order);
  }
}
