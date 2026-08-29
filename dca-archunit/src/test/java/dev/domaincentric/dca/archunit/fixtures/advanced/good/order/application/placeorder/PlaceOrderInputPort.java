package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.placeorder;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface PlaceOrderInputPort extends UseCase<PlaceOrderCommand, PlaceOrderResult> {}
