package dev.domaincentric.dca.archunit.fixtures.features.compat.sales.adapter.incoming.web.ordering;

import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.ordering.placeorder.PlaceOrderCommand;
import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.ordering.placeorder.PlaceOrderInputPort;
import java.util.UUID;
import org.springframework.stereotype.Controller;

@Controller
public final class OrderPageController {
  private final PlaceOrderInputPort placeOrder;

  public OrderPageController(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }

  public PlaceOrderPageViewModel place(UUID orderId) {
    return new PlaceOrderPageViewModel(
        placeOrder.execute(new PlaceOrderCommand(orderId)).orderId());
  }
}
