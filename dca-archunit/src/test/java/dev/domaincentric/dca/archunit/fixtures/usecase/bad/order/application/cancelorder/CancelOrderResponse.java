package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.cancelorder;

// DCA-USE-008: HTTP response model outside the incoming adapter
public record CancelOrderResponse(boolean cancelled) {}
