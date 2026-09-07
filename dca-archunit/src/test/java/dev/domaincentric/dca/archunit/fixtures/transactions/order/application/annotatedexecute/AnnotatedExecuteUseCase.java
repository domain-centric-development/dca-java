package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.annotatedexecute;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Valid: the executing method is transactional and delegates save and publish to a private helper
 * it calls — DCA-USE-009 and DCA-USE-012 follow the call within the class.
 */
@Service
public final class AnnotatedExecuteUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public AnnotatedExecuteUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  @Transactional
  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    persist(order);
    return order.id().value();
  }

  private void persist(Order order) {
    orders.save(order);
    events.publishAndClearEvents(order);
  }
}
