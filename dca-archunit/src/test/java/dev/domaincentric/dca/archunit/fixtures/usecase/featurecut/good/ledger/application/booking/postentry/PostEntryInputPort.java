package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.application.booking.postentry;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface PostEntryInputPort extends UseCase<PostEntryCommand, PostEntryResult> {}
