package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.placeorder.PlaceOrderInputPort;
import org.springframework.stereotype.Controller;

@Controller
public final class OrderPageController {
  private final PlaceOrderInputPort placeOrder;

  public OrderPageController(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }
}
