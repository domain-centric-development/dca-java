package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

/** DCA-TAC-001: named *AggregateRoot but does not implement the marker. */
public final class ShipmentAggregateRoot {
  private final String id;

  public ShipmentAggregateRoot(String id) {
    this.id = id;
  }

  public String id() {
    return id;
  }
}
