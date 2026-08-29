package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder;

import java.math.BigDecimal;

public record PlaceOrderCommand(BigDecimal total) {}
