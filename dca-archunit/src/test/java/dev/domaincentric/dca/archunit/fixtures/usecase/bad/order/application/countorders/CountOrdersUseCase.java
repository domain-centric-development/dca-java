package dev.domaincentric.dca.archunit.fixtures.usecase.bad.order.application.countorders;

// DCA-USE-021: a field written while it runs, shared by the next call. Its bare output is reported
// at the input port that binds it (DCA-USE-022), not again here.
public final class CountOrdersUseCase implements CountOrdersInputPort {
  private long lastCount;

  @Override
  public Long execute(CountOrdersQuery query) {
    lastCount = lastCount + 1;
    return lastCount;
  }
}
