package dev.domaincentric.dca.archunit.fixtures.naming.datawrites.bad.limits.application.updatestatus;

public final class UpdateStatusUseCase implements UpdateStatusInputPort {
  @Override
  public UpdateStatusResult execute(UpdateStatusCommand input) {
    return new UpdateStatusResult(input.reference());
  }
}
