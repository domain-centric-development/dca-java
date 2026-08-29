package dev.domaincentric.dca.archunit.fixtures.naming.bad.order.application.shiporder;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

// DCA-NAM-003: input port not ending with 'InputPort'
public interface ShipOrderPort extends UseCase<String, String> {}
