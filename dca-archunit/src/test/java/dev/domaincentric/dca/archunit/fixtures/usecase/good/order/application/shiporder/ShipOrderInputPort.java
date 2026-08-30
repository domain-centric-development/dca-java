package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.shiporder;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface ShipOrderInputPort extends UseCase<ShipOrderCommand, ShipOrderResult> {}
