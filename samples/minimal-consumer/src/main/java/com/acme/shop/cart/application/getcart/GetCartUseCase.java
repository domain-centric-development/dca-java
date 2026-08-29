package com.acme.shop.cart.application.getcart;

import com.acme.shop.cart.application.shared.ShoppingCartRepository;
import com.acme.shop.cart.domain.model.ShoppingCart;

public class GetCartUseCase implements GetCartInputPort {

  private final ShoppingCartRepository carts;

  public GetCartUseCase(ShoppingCartRepository carts) {
    this.carts = carts;
  }

  @Override
  public GetCartResult execute(GetCartQuery query) {
    ShoppingCart cart =
        carts.findById(query.cartId()).orElseThrow(() -> new IllegalArgumentException("no cart"));
    return new GetCartResult(
        cart.id().value(),
        cart.items().stream()
            .map(i -> new GetCartResult.Line(i.productId().value(), i.quantity().amount()))
            .toList());
  }
}
