package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.sharedhelper;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-009: {@code execute} saves and never publishes. The private {@code validate} helper it
 * shares with {@code separate} - which does publish - must not connect the two: a callee in common
 * is not an execution path.
 */
@Service
@Transactional
public final class SharedHelperUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public SharedHelperUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  public UUID execute(UUID orderId) {
    validate(orderId);
    Order order = new Order(new OrderId(orderId));
    orders.save(order);
    return order.id().value();
  }

  public void separate(UUID orderId) {
    validate(orderId);
    events.publishAndClearEvents(orders.findById(new OrderId(orderId)).orElseThrow());
  }

  private void validate(UUID orderId) {
    if (orderId == null) {
      throw new IllegalArgumentException("orderId");
    }
  }
}
