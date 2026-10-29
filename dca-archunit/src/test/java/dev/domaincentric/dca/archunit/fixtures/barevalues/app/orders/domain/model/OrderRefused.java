package dev.domaincentric.dca.archunit.fixtures.barevalues.app.orders.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainException;

/** A failure reports what was refused, the raw reason included. */
public final class OrderRefused extends DomainException {
  private final String reason;

  public OrderRefused(String reason) {
    super("Order refused: " + reason);
    this.reason = reason;
  }

  public String reason() {
    return reason;
  }
}
