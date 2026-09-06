package dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.shared;

import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface OrderRepository extends Repository<Order, OrderId> {}
