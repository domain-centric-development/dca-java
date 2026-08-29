package dev.domaincentric.dca.archunit.fixtures.naming.bad.order.application.placeorder;

// DCA-NAM-011: view model outside adapter.incoming.web
public record PlaceOrderViewModel(String orderId) {}
