package dev.domaincentric.dca.archunit.fixtures.strategic.bad.cart.events;

// Violates STR-013: a consumer-defined trigger without the event identity.
public interface ProductRemovalTrigger {

  String productId();
}
