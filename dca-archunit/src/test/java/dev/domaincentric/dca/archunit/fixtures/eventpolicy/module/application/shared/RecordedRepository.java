package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.shared;

public interface RecordedRepository
    extends dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository<
        dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.Recorded,
        dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.EntryId> {}
