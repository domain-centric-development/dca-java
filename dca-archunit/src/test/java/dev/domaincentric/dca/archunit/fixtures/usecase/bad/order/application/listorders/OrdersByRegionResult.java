package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;
import java.util.List;
import java.util.Map;

// DCA-USE-015: a nested container bound to the inherited type parameter
public final class OrdersByRegionResult extends GenericBase<Map<String, List<Order>>> {
  public OrdersByRegionResult(Map<String, List<Order>> byRegion) {
    super(byRegion, byRegion.size());
  }
}
