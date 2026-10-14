package dev.domaincentric.dca.archunit.fixtures.usecase.rawids.bad.ledger.application.closeaccount;

import java.util.UUID;

public final class CloseAccountUseCase implements CloseAccountInputPort {
  @Override
  public CloseAccountResult execute(CloseAccountCommand command) {
    UUID id = UUID.fromString(command.accountId());
    return new CloseAccountResult(id.toString());
  }
}
