package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.application.getorder;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface GetOrderInputPort extends UseCase<GetOrderQuery, GetOrderResult> {}
