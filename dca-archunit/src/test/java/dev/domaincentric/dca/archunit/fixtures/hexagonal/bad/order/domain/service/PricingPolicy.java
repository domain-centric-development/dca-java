package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.service;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainService;

public final class PricingPolicy implements DomainService {
  public Money withTax(Money net) {
    return net;
  }

  public static Money defaultTax(Money net) {
    return net;
  }
}
