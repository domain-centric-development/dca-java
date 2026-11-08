package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.model.OrderId;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.sharedkernel.domain.model.Money;

/** The domain service's outcome travels in the result; nothing outside the application calls it. */
public record PlaceOrderResult(OrderId orderId, Money totalWithTax) {}
