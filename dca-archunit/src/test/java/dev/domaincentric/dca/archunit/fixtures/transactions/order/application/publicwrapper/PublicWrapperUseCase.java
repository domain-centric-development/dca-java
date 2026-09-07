package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.publicwrapper;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-012: {@code execute} is public and publishes without a boundary. The transactional {@code
 * complete} that calls it does not cover a direct call of {@code execute}.
 */
@Service
public final class PublicWrapperUseCase {
  private final OrderRepository orders;
  private final DomainEventPublisher events;

  public PublicWrapperUseCase(OrderRepository orders, DomainEventPublisher events) {
    this.orders = orders;
    this.events = events;
  }

  @Transactional
  public UUID complete(UUID orderId) {
    return execute(new Order(new OrderId(orderId)));
  }

  public UUID execute(Order order) {
    orders.save(order);
    events.publishAndClearEvents(order);
    return order.id().value();
  }
}
