package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.shared;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.OutputPort;

public interface CarrierPort extends OutputPort {
  String quote(OrderId orderId);
}
