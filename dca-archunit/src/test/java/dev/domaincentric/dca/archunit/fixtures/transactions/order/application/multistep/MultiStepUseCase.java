package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.multistep;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Valid: save and publish each sit two delegation steps below the transactional entry method. */
@Service
public final class MultiStepUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public MultiStepUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  @Transactional
  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    store(order);
    broadcast(order);
    return order.id().value();
  }

  private void store(Order order) {
    persist(order);
  }

  private void persist(Order order) {
    orders.save(order);
  }

  private void broadcast(Order order) {
    announce(order);
  }

  private void announce(Order order) {
    events.publishAndClearEvents(order);
  }
}
