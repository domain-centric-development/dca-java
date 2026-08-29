package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.placeorder.PlaceOrderResult;

public final class OrderConverter {
  public OrderViewModel toViewModel(PlaceOrderResult result) {
    return new OrderViewModel(result.orderId().toString());
  }
}
