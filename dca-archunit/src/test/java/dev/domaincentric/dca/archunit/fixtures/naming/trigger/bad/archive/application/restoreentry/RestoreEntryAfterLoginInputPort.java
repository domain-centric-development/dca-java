package dev.domaincentric.dca.archunit.fixtures.naming.trigger.bad.archive.application.restoreentry;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface RestoreEntryAfterLoginInputPort
    extends UseCase<RestoreEntryAfterLoginCommand, RestoreEntryAfterLoginResult> {}
