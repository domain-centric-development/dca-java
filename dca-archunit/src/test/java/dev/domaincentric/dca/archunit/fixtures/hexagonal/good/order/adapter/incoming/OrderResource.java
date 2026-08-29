package dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.application.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.good.order.application.placeorder.PlaceOrderInputPort;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OrderResource {
  private final PlaceOrderInputPort placeOrder;

  public OrderResource(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }

  public String place(long cents) {
    return placeOrder.execute(new PlaceOrderCommand(cents)).orderId();
  }
}
