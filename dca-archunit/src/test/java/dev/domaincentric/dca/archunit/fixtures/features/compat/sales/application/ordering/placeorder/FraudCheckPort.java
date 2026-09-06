package dev.domaincentric.dca.archunit.fixtures.features.compat.sales.application.ordering.placeorder;

import dev.domaincentric.dca.archunit.fixtures.features.compat.sales.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

/** Use-case-local output port: only placing an order asks the (remote) fraud check. */
public interface FraudCheckPort extends OutputPort {
  boolean clears(OrderId orderId);
}
