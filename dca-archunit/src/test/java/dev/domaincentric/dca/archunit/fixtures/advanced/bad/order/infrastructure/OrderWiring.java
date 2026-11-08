package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.infrastructure;

import dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.service.OrderTotalCalculator;
import dev.domaincentric.dca.archunit.fixtures.advanced.bad.sharedkernel.domain.model.Money;
import java.math.BigDecimal;

/**
 * Violates DCA-ADV-021: the composition root may construct the domain service, but it calls it -
 * here to check a configured minimum at start-up.
 */
public final class OrderWiring {
  private final OrderTotalCalculator calculator = new OrderTotalCalculator(new BigDecimal("0.19"));

  public Money minimumWithTax(Money net) {
    return calculator.withTax(net);
  }
}
