package dev.domaincentric.dca.archunit.fixtures.usecase.rawids.good.ledger.application.closeaccount;

import dev.domaincentric.dca.archunit.fixtures.usecase.rawids.good.ledger.domain.model.AccountId;

public record CloseAccountCommand(AccountId accountId) {}
