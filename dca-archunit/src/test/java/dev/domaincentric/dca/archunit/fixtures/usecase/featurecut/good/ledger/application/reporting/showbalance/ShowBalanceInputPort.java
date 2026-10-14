package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.application.reporting.showbalance;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface ShowBalanceInputPort extends UseCase<ShowBalanceQuery, ShowBalanceResult> {}
