package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.savewithoutpublish;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-009: {@code execute} saves and never publishes; the publication in the unconnected {@code
 * republish} method must not count for it.
 */
@Service
@Transactional
public final class SaveWithoutPublishUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public SaveWithoutPublishUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    orders.save(order);
    return order.id().value();
  }

  public void republish(UUID orderId) {
    events.publishAndClearEvents(orders.findById(new OrderId(orderId)).orElseThrow());
  }
}
