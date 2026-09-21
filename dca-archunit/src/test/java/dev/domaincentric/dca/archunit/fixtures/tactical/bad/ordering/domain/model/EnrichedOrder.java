package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

/** A record that is not a Value; kept as a neighbour of the reported types. */
public record EnrichedOrder(OrderId orderId, int lineItemCount) {}
