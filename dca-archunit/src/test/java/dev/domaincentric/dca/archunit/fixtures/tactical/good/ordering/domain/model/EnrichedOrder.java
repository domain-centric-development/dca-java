package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.good.sharedkernel.domain.model.Money;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** Read projection combining an order with its total. */
public record EnrichedOrder(OrderId orderId, int lineItemCount, Money total) implements Value {}
