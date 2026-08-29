package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.adapter.outgoing;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.OrderId;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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
