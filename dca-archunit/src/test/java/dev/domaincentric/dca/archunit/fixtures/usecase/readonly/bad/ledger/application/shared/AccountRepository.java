package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.application.shared;

import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.domain.model.Account;
import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.domain.model.AccountId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Repository;

public interface AccountRepository extends Repository<Account, AccountId> {}
