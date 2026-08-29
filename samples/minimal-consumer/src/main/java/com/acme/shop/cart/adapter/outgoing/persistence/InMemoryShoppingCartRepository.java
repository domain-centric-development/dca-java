package com.acme.shop.cart.adapter.outgoing.persistence;

import com.acme.shop.cart.application.shared.ShoppingCartRepository;
import com.acme.shop.cart.domain.model.CartId;
import com.acme.shop.cart.domain.model.ShoppingCart;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryShoppingCartRepository implements ShoppingCartRepository {

  private final Map<CartId, ShoppingCart> store = new ConcurrentHashMap<>();

  @Override
  public Optional<ShoppingCart> findById(CartId id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public ShoppingCart save(ShoppingCart cart) {
    store.put(cart.id(), cart);
    return cart;
  }

  @Override
  public void deleteById(CartId id) {
    store.remove(id);
  }
}
