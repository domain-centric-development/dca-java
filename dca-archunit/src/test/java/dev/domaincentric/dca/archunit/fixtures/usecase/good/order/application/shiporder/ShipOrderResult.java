package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shiporder;

import java.util.UUID;

public record ShipOrderResult(UUID orderId, String carrierQuote) {}
