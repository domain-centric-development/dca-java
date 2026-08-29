package com.acme.shop.cart.application.shared;

import com.acme.shop.cart.domain.model.CartId;
import com.acme.shop.cart.domain.model.ShoppingCart;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface ShoppingCartRepository extends Repository<ShoppingCart, CartId> {}
