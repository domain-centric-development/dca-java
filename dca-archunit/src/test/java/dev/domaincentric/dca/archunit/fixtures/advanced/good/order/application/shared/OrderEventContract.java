package dev.domaincentric.dca.archunit.fixtures.advanced.good.order.application.shared;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;

/**
 * A contract several domain events share, declared outside the domain on purpose. The rules select
 * the types that carry the marker role, and an intermediate interface is not one of the concrete
 * events they govern: DCA-ADV-002 must not report it for residing outside a domain package.
 */
public interface OrderEventContract extends DomainEvent {}
