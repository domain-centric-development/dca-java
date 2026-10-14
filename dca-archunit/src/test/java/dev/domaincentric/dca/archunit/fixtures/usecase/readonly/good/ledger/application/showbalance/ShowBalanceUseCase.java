package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.application.showbalance;

import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.application.shared.AccountRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.application.shared.BalanceCache;
import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.domain.model.AccountId;

/** Answers what exists; when nothing exists it answers none. */
public final class ShowBalanceUseCase implements ShowBalanceInputPort {
  private final AccountRepository accounts;
  private final BalanceCache cache;

  public ShowBalanceUseCase(AccountRepository accounts, BalanceCache cache) {
    this.accounts = accounts;
    this.cache = cache;
  }

  @Override
  public ShowBalanceResult execute(ShowBalanceQuery query) {
    long cached = cache.lookup(query.account()).orElse(0L);
    return accounts
        .findById(new AccountId(query.account()))
        .map(account -> new ShowBalanceResult(account.id().value(), cached))
        .orElse(null);
  }
}
