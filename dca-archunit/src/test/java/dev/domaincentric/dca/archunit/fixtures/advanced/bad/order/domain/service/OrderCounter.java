package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.service;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainService;

public final class OrderCounter implements DomainService {
  private int count;

  public void increment() {
    count++;
  }
}
