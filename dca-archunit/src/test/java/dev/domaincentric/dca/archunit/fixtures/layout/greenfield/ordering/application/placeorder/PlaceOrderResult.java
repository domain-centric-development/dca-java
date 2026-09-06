package dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.domain.model.Order;
import java.util.UUID;

public record PlaceOrderResult(UUID orderId) {
  public static PlaceOrderResult from(Order order) {
    return new PlaceOrderResult(order.id().value());
  }
}
