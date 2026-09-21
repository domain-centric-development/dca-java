package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

/** DCA-TAC-016: bound to Order, named after Category. */
public interface CategoryRepository extends Repository<Order, OrderId> {}
