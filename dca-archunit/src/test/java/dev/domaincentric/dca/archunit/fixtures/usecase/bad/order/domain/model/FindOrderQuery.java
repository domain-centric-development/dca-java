package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model;

// DCA-USE-003: query outside the application layer
public record FindOrderQuery(OrderId orderId) {}
