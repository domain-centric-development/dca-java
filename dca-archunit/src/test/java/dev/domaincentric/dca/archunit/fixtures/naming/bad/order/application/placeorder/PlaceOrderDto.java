package dev.domaincentric.dca.archunit.fixtures.naming.bad.order.application.placeorder;

// DCA-NAM-007: DTO outside the adapter layer
public record PlaceOrderDto(String orderId) {}
