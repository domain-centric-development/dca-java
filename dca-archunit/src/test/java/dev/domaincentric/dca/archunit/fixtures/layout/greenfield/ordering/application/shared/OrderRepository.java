package dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.application.shared;

import dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.domain.model.Order;
import dev.domaincentric.dca.archunit.fixtures.layout.greenfield.ordering.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface OrderRepository extends Repository<Order, OrderId> {}
