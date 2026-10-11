package dev.domaincentric.dca.archunit.fixtures.strategic.good.cart.events;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;

/** A consumer-defined trigger: the event identity comes with the contract. */
public interface ProductAddedTrigger extends IntegrationEvent {

  String productId();

  /** Nested types belong to the enclosing contract and are not selected. */
  interface Line {
    int quantity();
  }
}
