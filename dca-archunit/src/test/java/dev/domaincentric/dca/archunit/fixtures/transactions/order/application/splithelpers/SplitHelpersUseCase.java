package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.splithelpers;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valid: {@code execute} saves through one helper and publishes through another. */
@Service
public final class SplitHelpersUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public SplitHelpersUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  @Transactional
  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    persist(order);
    announce(order);
    return order.id().value();
  }

  private void persist(Order order) {
    orders.save(order);
  }

  private void announce(Order order) {
    events.publishAndClearEvents(order);
  }
}
