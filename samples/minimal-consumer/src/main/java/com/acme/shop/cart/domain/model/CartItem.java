package com.acme.shop.cart.domain.model;

import com.acme.shop.sharedkernel.domain.model.ProductId;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

public record CartItem(ProductId productId, Quantity quantity) implements Value {}
