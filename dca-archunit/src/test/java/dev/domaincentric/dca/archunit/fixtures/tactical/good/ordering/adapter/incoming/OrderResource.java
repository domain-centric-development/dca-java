package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.placeorder.PlaceOrderInputPort;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.placeorder.PlaceOrderResult;

public class OrderResource {
  private final PlaceOrderInputPort placeOrder;

  public OrderResource(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }

  public PlaceOrderResult place(String orderId) {
    return placeOrder.execute(new PlaceOrderCommand(orderId));
  }
}
