package dev.domaincentric.dca.archunit.fixtures.naming.bad.order.application.shiporder;

// Not a DCA-NAM-002 finding: a record is a value carrier, not a use case, whatever its name.
public record ShipOrderUseCase(String orderId) {}
