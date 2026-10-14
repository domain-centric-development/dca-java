package dev.domaincentric.dca.archunit.fixtures.naming.trigger.good.archive.application.signon;

public final class SignOnUseCase implements SignOnInputPort {
  @Override
  public SignOnResult execute(SignOnCommand input) {
    return new SignOnResult(input.reference());
  }
}
