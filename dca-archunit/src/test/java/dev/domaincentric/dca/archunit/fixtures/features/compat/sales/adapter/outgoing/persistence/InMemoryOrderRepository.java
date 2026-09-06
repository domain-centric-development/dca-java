package dev.domaincentric.dca.archunit.fixtures.features.compat.sales.adapter.outgoing.persistence;

import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model.OrderId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public final class InMemoryOrderRepository implements OrderRepository {
  private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

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
