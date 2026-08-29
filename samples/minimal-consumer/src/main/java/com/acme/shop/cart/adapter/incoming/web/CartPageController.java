package com.acme.shop.cart.adapter.incoming.web;

import com.acme.shop.cart.application.getcart.GetCartInputPort;
import com.acme.shop.cart.application.getcart.GetCartQuery;
import com.acme.shop.cart.application.getcart.GetCartResult;
import com.acme.shop.cart.domain.model.CartId;
import java.util.UUID;

/** Framework-free stand-in for a web controller. */
public class CartPageController {

  private final GetCartInputPort getCart;

  public CartPageController(GetCartInputPort getCart) {
    this.getCart = getCart;
  }

  public GetCartResult show(UUID cartId) {
    return getCart.execute(new GetCartQuery(new CartId(cartId)));
  }
}
