package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.infrastructure.config.InMemoryConfig;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.model.OrderId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

// Violates HEX-005: outgoing adapter depends on an infrastructure implementation.
public class InMemoryOrderRepository implements OrderRepository {
  private final Map<OrderId, Order> store = new ConcurrentHashMap<>(InMemoryConfig.capacity());

  @Override
  public Optional<Order> findById(OrderId id) {
    return Optional.ofNullable(store.get(id));
  }

  @Override
  public Order save(Order aggregate) {
    store.put(aggregate.id(), aggregate);
    return aggregate;
  }

  @Override
  public void deleteById(OrderId id) {
    store.remove(id);
  }
}
