package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

/** DCA-TAC-016: named after Shipment, which is an entity, not an aggregate root. */
public interface ShipmentRepository extends Repository<Order, OrderId> {}
