package dev.domaincentric.dca.archunit.fixtures.usecase.rawids.good.ledger.application.closeaccount;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface CloseAccountInputPort extends UseCase<CloseAccountCommand, CloseAccountResult> {}
