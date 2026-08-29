package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.shared.OrderAuditStore;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model.OrderId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderAuditStore implements OrderAuditStore {
  private final Map<OrderId, Long> counts = new ConcurrentHashMap<>();

  @Override
  public void record(OrderId orderId, String message) {
    counts.merge(orderId, 1L, Long::sum);
  }

  @Override
  public long count(OrderId orderId) {
    return counts.getOrDefault(orderId, 0L);
  }
}
