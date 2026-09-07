package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.boundaryaround;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.application.TransactionBoundary;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Valid: save and publish inside the block handed to the boundary, in the same method. */
@Service
public final class BoundaryAroundUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;
  private final TransactionBoundary transactionBoundary;

  public BoundaryAroundUseCase(
      OrderRepository orders,
      DomainEventPublisher events,
      TransactionBoundary transactionBoundary) {
    this.orders = orders;
    this.events = events;
    this.transactionBoundary = transactionBoundary;
  }

  public UUID execute(UUID orderId) {
    return transactionBoundary.inTransaction(
        () -> {
          Order order = new Order(new OrderId(orderId));
          orders.save(order);
          events.publishAndClearEvents(order);
          return order.id().value();
        });
  }
}
