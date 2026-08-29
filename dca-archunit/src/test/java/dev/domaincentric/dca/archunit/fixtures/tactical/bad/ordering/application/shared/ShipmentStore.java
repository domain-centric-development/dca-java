package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

/** DCA-TAC-018: a *Store that extends Repository instead of Store. */
public interface ShipmentStore extends Repository<Order, OrderId> {}
