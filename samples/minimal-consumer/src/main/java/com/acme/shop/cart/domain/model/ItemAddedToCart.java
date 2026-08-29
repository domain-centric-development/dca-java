package com.acme.shop.cart.domain.model;

import com.acme.shop.sharedkernel.domain.model.ProductId;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record ItemAddedToCart(UUID eventId, Instant occurredOn, CartId cartId, ProductId productId)
    implements DomainEvent {}
