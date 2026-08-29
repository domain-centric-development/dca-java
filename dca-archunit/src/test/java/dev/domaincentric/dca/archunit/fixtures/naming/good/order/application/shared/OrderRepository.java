package dev.domaincentric.dca.archunit.fixtures.naming.good.order.application.shared;

import dev.domaincentric.dca.archunit.fixtures.naming.good.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.naming.good.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface OrderRepository extends Repository<Order, OrderId> {}
