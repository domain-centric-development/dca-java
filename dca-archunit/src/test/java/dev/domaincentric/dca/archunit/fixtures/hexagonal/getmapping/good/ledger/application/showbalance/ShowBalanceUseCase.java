package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.good.ledger.application.showbalance;

public final class ShowBalanceUseCase implements ShowBalanceInputPort {
  @Override
  public ShowBalanceResult execute(ShowBalanceQuery input) {
    return new ShowBalanceResult(input.reference());
  }
}
