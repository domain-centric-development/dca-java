package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface PostEntryInputPort extends UseCase<PostEntryCommand, PostEntryResult> {}
