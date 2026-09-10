package dev.domaincentric.dca.archunit.fixtures.construction.alpha.domain;

public class LineFactory implements dev.domaincentric.dca.buildingblocks.ddd.tactical.Factory {
  public static Line reconstitute() {
    return new Line(new LineId("x"));
  }
}
