package dev.domaincentric.dca.archunit.fixtures.naming.datawrites.good.limits.application.raiselimit;

public final class RaiseLimitUseCase implements RaiseLimitInputPort {
  @Override
  public RaiseLimitResult execute(RaiseLimitCommand input) {
    return new RaiseLimitResult(input.reference());
  }
}
