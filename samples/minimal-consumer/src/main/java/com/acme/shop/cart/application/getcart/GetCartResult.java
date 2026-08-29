package com.acme.shop.cart.application.getcart;

import java.util.List;
import java.util.UUID;

public record GetCartResult(UUID cartId, List<Line> lines) {
  public record Line(UUID productId, int quantity) {}
}
