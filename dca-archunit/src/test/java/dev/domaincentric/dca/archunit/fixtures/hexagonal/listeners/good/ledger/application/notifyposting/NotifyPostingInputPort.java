package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.application.notifyposting;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface NotifyPostingInputPort
    extends UseCase<NotifyPostingCommand, NotifyPostingResult> {}
