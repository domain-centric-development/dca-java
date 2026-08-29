package dev.domaincentric.dca.archunit.fixtures.naming.good.order.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.naming.good.order.application.placeorder.PlaceOrderResult;

public final class OrderConverter {
  public OrderViewModel toViewModel(PlaceOrderResult result) {
    return new OrderViewModel(result.orderId().toString());
  }
}
