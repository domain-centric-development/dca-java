package dev.domaincentric.dca.archunit.modulith.fixture.shipping;

import dev.domaincentric.dca.archunit.modulith.fixture.orders.Order;
import dev.domaincentric.dca.archunit.modulith.fixture.shipping.internal.Label;

/** Exposed type of the shipping module; depends on the exposed Order and on its own internals. */
public record Shipment(Order order, Label label) {}
