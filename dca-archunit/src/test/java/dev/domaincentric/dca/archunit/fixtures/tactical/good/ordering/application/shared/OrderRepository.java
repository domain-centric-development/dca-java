package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends Repository<Order, OrderId> {
  List<Order> findAll();

  Optional<Order> findLatest();

  long count();
}
