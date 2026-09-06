package dev.domaincentric.dca.archunit.fixtures.features.compat.sales.adapter.outgoing.fraud;

import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.ordering.placeorder.FraudCheckPort;
import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model.OrderId;
import org.springframework.stereotype.Component;

@Component
public final class AlwaysClearsFraudCheck implements FraudCheckPort {
  @Override
  public boolean clears(OrderId orderId) {
    return true;
  }
}
