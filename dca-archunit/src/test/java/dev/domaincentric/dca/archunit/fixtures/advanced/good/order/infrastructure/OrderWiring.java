package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.infrastructure;

import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.adapter.outgoing.InMemoryOrderRepository;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder.PlaceOrderInputPort;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder.PlaceOrderUseCase;
import dev.domaincentric.dca.archunit.fixtures.advanced.good.order.domain.service.OrderTotalCalculator;
import java.math.BigDecimal;

/**
 * DCA-ADV-021: the composition root constructs the domain service and hands it to the use case - it
 * calls nothing on it.
 */
public final class OrderWiring {
  public PlaceOrderInputPort placeOrder() {
    return new PlaceOrderUseCase(
        new InMemoryOrderRepository(), new OrderTotalCalculator(new BigDecimal("0.19")));
  }
}
