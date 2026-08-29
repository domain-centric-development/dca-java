package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder.OrderRepository;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.OrderId;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Shipment;
import java.util.Optional;

/** DCA-TAC-015: repository implementation outside adapter.outgoing. */
public class InMemoryOrderRepository implements OrderRepository {
  @Override
  public Optional<Order> findById(OrderId id) {
    return Optional.empty();
  }

  @Override
  public Order save(Order aggregate) {
    return aggregate;
  }

  @Override
  public void deleteById(OrderId id) {}

  @Override
  public Optional<Shipment> findShipment(OrderId id) {
    return Optional.empty();
  }
}
