package dev.domaincentric.dca.archunit.fixtures.transactions.order.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.transactions.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.transactions.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort;
import java.util.UUID;

/**
 * DCA-USE-009 / DCA-USE-012 / DCA-USE-013 select classes in an application package only. This one
 * implements the input-port role, saves an aggregate, publishes nothing and carries no transaction
 * boundary — and sits in an incoming adapter, so none of the three may report it. It would be
 * reported if the selection were read as {@code ((inApplication ∧ suffix) ∨ isInputPort)}.
 */
public final class OutOfLayerInputPort implements InputPort {
  private final OrderRepository orders;

  public OutOfLayerInputPort(OrderRepository orders) {
    this.orders = orders;
  }

  public UUID save(UUID orderId) {
    Order order = new Order(new OrderId(orderId));
    orders.save(order);
    return order.id().value();
  }
}
