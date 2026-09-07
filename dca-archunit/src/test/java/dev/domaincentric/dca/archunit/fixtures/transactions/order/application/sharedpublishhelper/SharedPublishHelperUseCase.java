package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.sharedpublishhelper;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-012: two entry methods share the publishing helper {@code announce}. {@code execute} is
 * transactional, {@code executeQuietly} is not - the annotated caller must not cover the other
 * path.
 */
@Service
public final class SharedPublishHelperUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public SharedPublishHelperUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  @Transactional
  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    orders.save(order);
    announce(order);
    return order.id().value();
  }

  public void executeQuietly(UUID orderId) {
    announce(orders.findById(new OrderId(orderId)).orElseThrow());
  }

  private void announce(Order order) {
    events.publishAndClearEvents(order);
  }
}
