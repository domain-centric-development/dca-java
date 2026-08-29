package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.shared.OrderAuditStore;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model.OrderId;

public class PlaceOrderUseCase implements PlaceOrderInputPort {
  private final OrderRepository orders;
  private final OrderAuditStore audit;

  public PlaceOrderUseCase(OrderRepository orders, OrderAuditStore audit) {
    this.orders = orders;
    this.audit = audit;
  }

  @Override
  public PlaceOrderResult execute(PlaceOrderCommand command) {
    OrderId id = new OrderId(command.orderId());
    Order order = orders.findById(id).orElseThrow();
    order.place();
    orders.save(order);
    audit.record(id, "placed");
    return new PlaceOrderResult(id.value(), order.isPlaced());
  }
}
