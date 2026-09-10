package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.savenoboundary;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * DCA-USE-012: {@code execute} saves and {@code remove} deletes an aggregate without any
 * transaction boundary - no annotation on the class or the methods, no {@code inTransaction}. The
 * repository may write the aggregate as several statements; the use case owns the boundary even
 * when no event is published.
 */
@Service
public final class SaveNoBoundaryUseCase {
  private final OrderRepository orders;

  public SaveNoBoundaryUseCase(OrderRepository orders) {
    this.orders = orders;
  }

  public UUID execute(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    orders.save(order);
    return order.id().value();
  }

  public void remove(UUID orderId) {
    orders.deleteById(new OrderId(orderId));
  }
}
