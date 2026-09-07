package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Entity;

/**
 * DCA-TAC-004 (no Id-typed field), DCA-TAC-005 (public constructor), DCA-TAC-006 (inherited public
 * setter), DCA-TAC-007 (field of aggregate-root type, and an array of aggregate roots).
 */
public final class Shipment extends TrackedEntity implements Entity<Shipment, ShipmentId> {
  private final String id;
  private final Order order;
  private final Customer[] recipients = new Customer[0];

  public Shipment(String id, Order order) {
    this.id = id;
    this.order = order;
  }

  @Override
  public ShipmentId id() {
    return new ShipmentId(id);
  }

  public Order order() {
    return order;
  }

  public Customer[] recipients() {
    return recipients;
  }
}
