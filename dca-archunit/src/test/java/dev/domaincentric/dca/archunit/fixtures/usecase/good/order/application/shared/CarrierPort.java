package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shared;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

/** Remote-capable output port (a carrier's API) — must be called outside the transaction. */
public interface CarrierPort extends OutputPort {
  String quote(OrderId orderId);
}
