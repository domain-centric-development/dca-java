package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.mutualrecursion;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DCA-USE-009: {@code ping} and {@code pong} call each other, {@code ping} saves, nothing
 * publishes. No method of the cycle is an entry point; the rule must still terminate and report.
 */
@Service
@Transactional
public final class MutualRecursionUseCase {
  private final OrderRepository orders;

  public MutualRecursionUseCase(OrderRepository orders) {
    this.orders = orders;
  }

  private void ping(Order order, int depth) {
    orders.save(order);
    if (depth > 0) {
      pong(order, depth - 1);
    }
  }

  private void pong(Order order, int depth) {
    if (depth > 0) {
      ping(order, depth - 1);
    }
  }
}
