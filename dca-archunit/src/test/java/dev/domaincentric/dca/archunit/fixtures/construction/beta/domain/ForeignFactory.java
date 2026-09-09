package dev.domaincentric.dca.archunit.fixtures.construction.beta.domain;

public class ForeignFactory implements dev.domaincentric.dca.buildingblocks.ddd.tactical.Factory {
  public Object create() {
    return new dev.domaincentric.dca.archunit.fixtures.construction.alpha.domain.Line(null);
  }
}
