package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.showbalance;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.in.UseCase;

public interface ShowBalanceInputPort extends UseCase<ShowBalanceQuery, ShowBalanceResult> {}
