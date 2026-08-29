package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.application.placeorder;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface PlaceOrderInputPort extends UseCase<PlaceOrderCommand, PlaceOrderResult> {}
