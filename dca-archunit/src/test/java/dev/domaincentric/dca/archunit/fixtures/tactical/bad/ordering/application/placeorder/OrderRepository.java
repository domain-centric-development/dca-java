package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.OrderId;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Shipment;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;
import java.util.Optional;

/** DCA-TAC-014 (not in application.shared), DCA-TAC-017 (returns a non-root entity). */
public interface OrderRepository extends Repository<Order, OrderId> {
  Optional<Shipment> findShipment(OrderId id);
}
