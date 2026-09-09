package dev.domaincentric.dca.archunit.fixtures.construction.alpha.adapter.outgoing;

public class Persistence {
  public Object load() {
    return dev.domaincentric.dca.archunit.fixtures.construction.alpha.domain.LineFactory
        .reconstitute();
  }

  public Object bypass() {
    return new dev.domaincentric.dca.archunit.fixtures.construction.alpha.domain.Line(null);
  }
}
