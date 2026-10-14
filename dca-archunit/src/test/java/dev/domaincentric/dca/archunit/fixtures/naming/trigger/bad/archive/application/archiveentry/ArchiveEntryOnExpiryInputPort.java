package dev.domaincentric.dca.archunit.fixtures.naming.trigger.bad.archive.application.archiveentry;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface ArchiveEntryOnExpiryInputPort
    extends UseCase<ArchiveEntryOnExpiryCommand, ArchiveEntryOnExpiryResult> {}
