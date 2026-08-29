package dev.domaincentric.dca.archunit.fixtures.hexagonal.bad.order.domain.model;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;

// Violates HEX-010: output port declared in the domain layer.
public interface OrderStore extends Store {}
