package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.Factory;

public final class SequencedOrderFactory implements Factory {
  private long sequence;

  public long next() {
    return ++sequence;
  }
}
