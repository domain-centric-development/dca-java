package dev.domaincentric.dca.archunit.fixtures.naming.trigger.good.archive.application.archiveentry;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface ArchiveEntryInputPort extends UseCase<ArchiveEntryCommand, ArchiveEntryResult> {}
