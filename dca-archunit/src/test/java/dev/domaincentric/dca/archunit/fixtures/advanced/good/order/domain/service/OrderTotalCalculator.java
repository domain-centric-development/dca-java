package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.service;

import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainService;
import java.math.BigDecimal;

public final class OrderTotalCalculator implements DomainService {
  private final BigDecimal taxRate;

  public OrderTotalCalculator(BigDecimal taxRate) {
    this.taxRate = taxRate;
  }

  /** Takes the aggregate, not extracted values - see DCA-ADV-020. */
  public Money totalWithTax(Order order) {
    return new Money(order.total().amount().multiply(BigDecimal.ONE.add(taxRate)));
  }
}
