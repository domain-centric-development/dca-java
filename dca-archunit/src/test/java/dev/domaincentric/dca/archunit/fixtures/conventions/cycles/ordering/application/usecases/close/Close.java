package dev.domaincentric.dca.archunit.fixtures.conventions.cycles.ordering.application.usecases.close;

public class Close implements dev.domaincentric.dca.buildingblocks.hexagonal.port.in.InputPort {
  public dev.domaincentric.dca.archunit.fixtures.conventions.cycles.ordering.application.usecases
          .open.Open
      other;
}
