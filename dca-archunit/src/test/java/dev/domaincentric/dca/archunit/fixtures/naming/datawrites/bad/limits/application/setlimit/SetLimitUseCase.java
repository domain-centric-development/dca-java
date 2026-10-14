package dev.domaincentric.dca.archunit.fixtures.naming.datawrites.bad.limits.application.setlimit;

public final class SetLimitUseCase implements SetLimitInputPort {
  @Override
  public SetLimitResult execute(SetLimitCommand input) {
    return new SetLimitResult(input.reference());
  }
}
