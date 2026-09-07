package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.sharedsavehelper;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-009: two entry methods share the saving helper {@code persist}. {@code execute} publishes
 * after it, {@code executeQuietly} does not - the covered caller must not cover the other.
 */
@Service
@Transactional
public final class SharedSaveHelperUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public SharedSaveHelperUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    persist(order);
    events.publishAndClearEvents(order);
    return order.id().value();
  }

  public UUID executeQuietly(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    persist(order);
    return order.id().value();
  }

  private void persist(Order order) {
    orders.save(order);
  }
}
