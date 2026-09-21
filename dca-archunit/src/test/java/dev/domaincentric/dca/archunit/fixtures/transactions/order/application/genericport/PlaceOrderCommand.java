package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.genericport;

import java.util.UUID;

public record PlaceOrderCommand(UUID orderId) {}
