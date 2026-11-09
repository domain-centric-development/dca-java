package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.countorders;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

// DCA-USE-022: answers with a bare count whose meaning lives in the caller.
public interface CountOrdersInputPort extends UseCase<CountOrdersQuery, Long> {}
