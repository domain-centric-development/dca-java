package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder;

public class DirectConstruction {
  public Object execute() {
    return new dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Shipment(
        "x", null);
  }
}
