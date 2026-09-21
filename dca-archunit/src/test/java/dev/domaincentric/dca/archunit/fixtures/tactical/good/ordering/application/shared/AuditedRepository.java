package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.application.shared;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.AggregateRoot;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Id;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

/**
 * A generic intermediate port: it binds no aggregate of its own, so DCA-TAC-016 skips it instead of
 * looking for a class named "Audited".
 */
public interface AuditedRepository<T extends AggregateRoot<T, ID>, ID extends Id>
    extends Repository<T, ID> {}
