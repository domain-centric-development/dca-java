package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.application.showbalance;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface ShowBalanceInputPort extends UseCase<ShowBalanceQuery, ShowBalanceResult> {}
