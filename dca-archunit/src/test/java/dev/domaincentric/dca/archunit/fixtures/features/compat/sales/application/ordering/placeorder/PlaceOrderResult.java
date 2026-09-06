package dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.ordering.placeorder;

import java.util.UUID;

public record PlaceOrderResult(UUID orderId, boolean cleared) {}
