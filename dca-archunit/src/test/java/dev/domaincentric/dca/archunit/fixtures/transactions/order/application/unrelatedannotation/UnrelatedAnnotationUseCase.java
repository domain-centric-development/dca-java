package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.unrelatedannotation;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-012: {@code execute} publishes without a transaction; the {@code @Transactional} on the
 * unrelated {@code audit} method must not count for it.
 */
@Service
public final class UnrelatedAnnotationUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public UnrelatedAnnotationUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    orders.save(order);
    events.publishAndClearEvents(order);
    return order.id().value();
  }

  @Transactional
  public void audit(UUID orderId) {
    orders.findById(new OrderId(orderId));
  }
}
