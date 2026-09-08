package dev.domaincentric.dca.archunit.springmodulith.fixture.shipping;

import dev.domaincentric.dca.archunit.springmodulith.fixture.orders.Order;
import dev.domaincentric.dca.archunit.springmodulith.fixture.shipping.internal.Label;

/** Exposed type of the shipping module; depends on the exposed Order and on its own internals. */
public record Shipment(Order order, Label label) {}
