package dev.domaincentric.dca.archunit.fixtures.construction.alpha.domain;

public class OtherAggregate
    extends dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot<
        OtherAggregate, LineId> {
  public LineId id() {
    return new LineId("other");
  }

  public Line create() {
    return new Line(new LineId("x"));
  }
}
