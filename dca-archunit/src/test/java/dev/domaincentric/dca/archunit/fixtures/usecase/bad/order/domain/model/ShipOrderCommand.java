package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model;

// DCA-USE-002: command outside the application layer
public record ShipOrderCommand(OrderId orderId) {}
