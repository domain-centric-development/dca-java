package dev.domaincentric.dca.archunit.fixtures.transactions.order.application.genericport;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

/** The ordinary shape: a named input port over the generic use-case marker. */
public interface PlaceOrderInputPort extends UseCase<PlaceOrderCommand, PlaceOrderResult> {}
