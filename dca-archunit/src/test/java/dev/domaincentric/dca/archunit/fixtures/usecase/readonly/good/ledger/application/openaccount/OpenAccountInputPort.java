package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.application.openaccount;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface OpenAccountInputPort extends UseCase<OpenAccountCommand, OpenAccountResult> {}
