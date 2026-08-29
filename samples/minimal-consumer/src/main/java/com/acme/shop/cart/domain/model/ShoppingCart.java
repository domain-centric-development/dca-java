package com.acme.shop.cart.domain.model;

import com.acme.shop.sharedkernel.domain.model.ProductId;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShoppingCart extends BaseAggregateRoot<ShoppingCart, CartId> {

  private final CartId id;
  private final List<CartItem> items = new ArrayList<>();

  private ShoppingCart(CartId id) {
    this.id = id;
  }

  public static ShoppingCart create() {
    return new ShoppingCart(CartId.generate());
  }

  @Override
  public CartId id() {
    return id;
  }

  public List<CartItem> items() {
    return List.copyOf(items);
  }

  public void addItem(ProductId productId, Quantity quantity) {
    items.add(new CartItem(productId, quantity));
    registerEvent(new ItemAddedToCart(UUID.randomUUID(), Instant.now(), id, productId));
  }
}
