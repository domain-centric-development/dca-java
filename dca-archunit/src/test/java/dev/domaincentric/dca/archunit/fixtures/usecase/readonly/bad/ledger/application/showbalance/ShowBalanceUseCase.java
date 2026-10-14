package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.application.showbalance;

import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.application.shared.AccountRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.application.shared.BalanceCache;
import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.domain.model.Account;
import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.bad.ledger.domain.model.AccountId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;

/** A read that creates: find-or-create, a putBalanceed balance and a publication. */
public final class ShowBalanceUseCase implements ShowBalanceInputPort {
  private final AccountRepository accounts;
  private final BalanceCache cache;
  private final DomainEventPublisher events;

  public ShowBalanceUseCase(
      AccountRepository accounts, BalanceCache cache, DomainEventPublisher events) {
    this.accounts = accounts;
    this.cache = cache;
    this.events = events;
  }

  @Override
  public ShowBalanceResult execute(ShowBalanceQuery query) {
    Account account =
        accounts.findById(new AccountId(query.account())).orElseGet(() -> openAccount(query));
    cache.putBalance(query.account(), 0L);
    return new ShowBalanceResult(account.id().value(), 0L);
  }

  private Account openAccount(ShowBalanceQuery query) {
    Account account = accounts.save(Account.open(new AccountId(query.account())));
    events.publishAndClearEvents(account);
    return account;
  }
}
