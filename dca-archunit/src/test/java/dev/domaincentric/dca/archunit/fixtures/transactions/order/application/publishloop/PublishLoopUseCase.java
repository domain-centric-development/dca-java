package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.publishloop;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-009: saves, then dispatches the events by hand - {@code publish(event)} per event and an
 * explicit {@code clearDomainEvents()}. Only {@code publishAndClearEvents} is a publication; this
 * shape is reported although every event does get published.
 */
@Service
@Transactional
public final class PublishLoopUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public PublishLoopUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    orders.save(order);
    for (DomainEvent event : order.domainEvents()) {
      events.publish(event);
    }
    order.clearDomainEvents();
    return order.id().value();
  }
}
