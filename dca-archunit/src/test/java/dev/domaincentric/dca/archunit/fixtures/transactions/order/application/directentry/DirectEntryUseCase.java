package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.directentry;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-009: {@code execute} is public and saves without publishing. That {@code complete} wraps
 * it and publishes afterwards does not help a caller who invokes {@code execute} directly - a
 * public method stays an entry point even when another method calls it.
 */
@Service
@Transactional
public final class DirectEntryUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public DirectEntryUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  public UUID complete(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    execute(order);
    events.publishAndClearEvents(order);
    return order.id().value();
  }

  public void execute(Order order) {
    orders.save(order);
  }
}
