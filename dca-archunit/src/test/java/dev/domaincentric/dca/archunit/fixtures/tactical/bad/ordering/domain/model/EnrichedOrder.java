package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.domain.model;

/** DCA-TAC-022: a record, but not a Value. */
public record EnrichedOrder(OrderId orderId, int lineItemCount) {}
