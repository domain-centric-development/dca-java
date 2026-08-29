package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.service;

import dev.domaincentric.dca.archunit.fixtures.advanced.good.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainService;
import java.math.BigDecimal;

public final class OrderTotalCalculator implements DomainService {
  private final BigDecimal taxRate;

  public OrderTotalCalculator(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  public Money withTax(Money net) {
    return new Money(net.amount().multiply(BigDecimal.ONE.add(taxRate)));
  }
}
