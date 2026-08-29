package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;

public interface OrderAuditStore extends Store {
  void record(OrderId orderId, String message);

  long count(OrderId orderId);
}
