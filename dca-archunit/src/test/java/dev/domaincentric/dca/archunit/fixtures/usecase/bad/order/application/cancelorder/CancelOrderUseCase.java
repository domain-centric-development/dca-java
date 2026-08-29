package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.cancelorder;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.adapter.incoming.web.OrderDto;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.OrderId;
import org.springframework.stereotype.Service;

// DCA-USE-009: saves without publishing; DCA-USE-011: depends on a DTO
@Service
public final class CancelOrderUseCase {
  private final OrderRepository orders;

  public CancelOrderUseCase(OrderRepository orders) {
    this.orders = orders;
  }

  public OrderDto execute(CancelOrderCommand command) {
    Order order = Order.place(new OrderId(command.orderId));
    orders.save(order);
    return new OrderDto(order.id().value());
  }
}
