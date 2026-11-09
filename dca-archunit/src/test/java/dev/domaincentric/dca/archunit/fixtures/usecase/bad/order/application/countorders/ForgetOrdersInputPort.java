package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.countorders;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

/** A command with nothing to answer: Void passes DCA-USE-022. */
public interface ForgetOrdersInputPort extends UseCase<CountOrdersQuery, Void> {}
