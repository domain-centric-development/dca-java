package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.recursivesave;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valid: the saving helper calls itself; the entry method publishes. The walk must terminate. */
@Service
public final class RecursiveSaveUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public RecursiveSaveUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  @Transactional
  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    persistWithRetry(order, 3);
    events.publishAndClearEvents(order);
    return order.id().value();
  }

  private void persistWithRetry(Order order, int attemptsLeft) {
    if (attemptsLeft > 1) {
      persistWithRetry(order, attemptsLeft - 1);
    }
    orders.save(order);
  }
}
