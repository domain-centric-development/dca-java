package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.shared;

public interface EntryRepository
    extends dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository<
        dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.Entry,
        dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.EntryId> {}
