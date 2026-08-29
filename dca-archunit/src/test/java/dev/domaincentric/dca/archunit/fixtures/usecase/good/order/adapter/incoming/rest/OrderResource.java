package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.adapter.incoming.rest;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.placeorder.PlaceOrderInputPort;
import java.util.UUID;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class OrderResource {
  private final PlaceOrderInputPort placeOrder;

  public OrderResource(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }

  public OrderResponse place(UUID id) {
    return new OrderResponse(placeOrder.execute(new PlaceOrderCommand(id)).orderId());
  }
}
