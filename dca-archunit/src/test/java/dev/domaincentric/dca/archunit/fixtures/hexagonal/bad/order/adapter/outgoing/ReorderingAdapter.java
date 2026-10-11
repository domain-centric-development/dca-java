package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderInputPort;

// Violates HEX-013: an outgoing adapter that drives the application through an input port.
public class ReorderingAdapter {
  private final PlaceOrderInputPort placeOrder;

  public ReorderingAdapter(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }
}
