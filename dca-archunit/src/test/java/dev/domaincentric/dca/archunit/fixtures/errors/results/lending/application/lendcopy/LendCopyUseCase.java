package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy;

import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.domain.model.LoanLimitReachedException;

public final class LendCopyUseCase implements LendCopyInputPort {

  @Override
  public LendCopyResult execute(LendCopyCommand command) {
    try {
      if (command.copyId().isBlank()) {
        throw new LoanLimitReachedException(3);
      }
      return new LendCopyResult.Lent(command.memberId() + "-" + command.copyId());
    } catch (LoanLimitReachedException refused) {
      return new LendCopyResult.LimitReached(3);
    }
  }
}
