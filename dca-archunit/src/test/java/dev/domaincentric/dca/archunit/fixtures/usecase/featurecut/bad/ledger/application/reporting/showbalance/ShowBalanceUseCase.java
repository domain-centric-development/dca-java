package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.bad.ledger.application.reporting.showbalance;

public final class ShowBalanceUseCase implements ShowBalanceInputPort {
  @Override
  public ShowBalanceResult execute(ShowBalanceQuery input) {
    return new ShowBalanceResult(input.reference());
  }
}
