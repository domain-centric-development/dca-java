package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.countorders;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

/** Leaves the output open: an unresolved type argument passes DCA-USE-022. */
public interface Answering<R> extends UseCase<CountOrdersQuery, R> {}
