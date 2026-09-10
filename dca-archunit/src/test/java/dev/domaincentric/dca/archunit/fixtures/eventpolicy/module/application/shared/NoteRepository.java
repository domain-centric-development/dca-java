package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.application.shared;

public interface NoteRepository
    extends dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository<
        dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.Note,
        dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model.EntryId> {}
