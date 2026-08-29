package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model.OrderId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {
  private final Map<OrderId, Order> storage = new ConcurrentHashMap<>();

  @Override
  public Optional<Order> findById(OrderId id) {
    return Optional.ofNullable(storage.get(id));
  }

  @Override
  public Order save(Order aggregate) {
    storage.put(aggregate.id(), aggregate);
    return aggregate;
  }

  @Override
  public void deleteById(OrderId id) {
    storage.remove(id);
  }

  @Override
  public List<Order> findAll() {
    return new ArrayList<>(storage.values());
  }

  @Override
  public Optional<Order> findLatest() {
    return storage.values().stream().findFirst();
  }

  @Override
  public long count() {
    return storage.size();
  }
}
