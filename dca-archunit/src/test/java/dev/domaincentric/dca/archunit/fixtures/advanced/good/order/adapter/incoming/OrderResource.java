package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder.PlaceOrderInputPort;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder.PlaceOrderResult;

public final class OrderResource {
  private final PlaceOrderInputPort placeOrder;

  public OrderResource(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }

  public PlaceOrderResult place(PlaceOrderCommand command) {
    return placeOrder.execute(command);
  }
}
