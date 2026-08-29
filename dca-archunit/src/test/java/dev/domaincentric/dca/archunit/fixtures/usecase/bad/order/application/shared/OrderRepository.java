package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shared;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface OrderRepository extends Repository<Order, OrderId> {}
