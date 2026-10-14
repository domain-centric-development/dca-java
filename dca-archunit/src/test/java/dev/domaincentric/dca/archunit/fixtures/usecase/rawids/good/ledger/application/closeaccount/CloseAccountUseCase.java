package dev.domaincentric.dca.archunit.fixtures.usecase.rawids.good.ledger.application.closeaccount;

public final class CloseAccountUseCase implements CloseAccountInputPort {
  @Override
  public CloseAccountResult execute(CloseAccountCommand command) {
    return new CloseAccountResult(command.accountId().value().toString());
  }
}
