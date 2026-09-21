package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.listorders;

import dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.domain.model.Order;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/**
 * A domain value object that happens to end in Result and sits in an application package.
 * DCA-USE-006 already lets it cross the port, so DCA-USE-015 does not walk it either - the .NET
 * twin has exempted the value role from the start.
 */
public record MoneyResult(Order order) implements Value {}
