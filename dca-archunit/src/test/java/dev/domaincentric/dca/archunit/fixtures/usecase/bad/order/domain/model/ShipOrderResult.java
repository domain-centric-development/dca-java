package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model;

// DCA-USE-006: non-Value result outside the application layer
public record ShipOrderResult(OrderId orderId) {}
