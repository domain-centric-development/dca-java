package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.domain.service.OrderTotalCalculator;
import dev.domaincentric.dca.archunit.fixtures.advanced.bad.sharedkernel.domain.model.Money;
import java.math.BigDecimal;

/**
 * Violates DCA-ADV-021: an outgoing adapter computes with a domain service while it writes - the
 * decision the use case owns happens at the edge.
 */
public final class OrderTotalExport {
  public String line(Money net) {
    return new OrderTotalCalculator(BigDecimal.ONE).withTax(net).toString();
  }
}
