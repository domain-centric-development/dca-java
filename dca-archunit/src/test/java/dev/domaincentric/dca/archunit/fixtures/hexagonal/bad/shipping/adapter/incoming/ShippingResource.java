package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.shipping.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder.PlaceOrderInputPort;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.service.PricingPolicy;

// Violates HEX-007 (incoming adapter reaches into another bounded context) and HEX-012 (static
// access to a domain service).
public class ShippingResource {
  private final PlaceOrderInputPort placeOrder;

  public ShippingResource(PlaceOrderInputPort placeOrder) {
    this.placeOrder = placeOrder;
  }

  public String status() {
    return placeOrder.toString() + PricingPolicy.defaultTax(null);
  }
}
