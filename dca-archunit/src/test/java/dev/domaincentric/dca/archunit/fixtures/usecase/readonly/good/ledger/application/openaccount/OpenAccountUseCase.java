package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.application.openaccount;

import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.application.shared.AccountRepository;
import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.domain.model.Account;
import dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.domain.model.AccountId;
import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.DomainEventPublisher;

public final class OpenAccountUseCase implements OpenAccountInputPort {
  private final AccountRepository accounts;
  private final DomainEventPublisher events;

  public OpenAccountUseCase(AccountRepository accounts, DomainEventPublisher events) {
    this.accounts = accounts;
    this.events = events;
  }

  @Override
  public OpenAccountResult execute(OpenAccountCommand command) {
    Account account = accounts.save(Account.open(new AccountId(command.account())));
    events.publishAndClearEvents(account);
    return new OpenAccountResult(account.id().value());
  }
}
