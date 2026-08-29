package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.shared;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

/** DCA-TAC-016: there is no class named Phantom in this context — must fail, not pass silently. */
public interface PhantomRepository extends Repository<Order, OrderId> {}
