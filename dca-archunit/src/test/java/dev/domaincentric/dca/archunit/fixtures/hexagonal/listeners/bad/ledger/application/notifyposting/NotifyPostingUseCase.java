package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.bad.ledger.application.notifyposting;

public final class NotifyPostingUseCase implements NotifyPostingInputPort {
  @Override
  public NotifyPostingResult execute(NotifyPostingCommand input) {
    return new NotifyPostingResult(true);
  }
}
