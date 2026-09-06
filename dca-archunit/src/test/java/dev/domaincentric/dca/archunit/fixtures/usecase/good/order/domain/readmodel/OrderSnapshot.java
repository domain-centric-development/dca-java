package dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.readmodel;

import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.Money;
import dev.domaincentric.dca.archunit.fixtures.usecase.good.order.domain.model.OrderId;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** Read model: a value snapshot of the aggregate's state, safe to hand out through a result. */
public record OrderSnapshot(OrderId orderId, Money total) implements Value {}
