package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.countorders;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;
import java.util.List;

/** A container of text is no bare primitive: passes DCA-USE-022. */
public interface ListOrderRegionsInputPort extends UseCase<CountOrdersQuery, List<String>> {}
