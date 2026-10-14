package dev.domaincentric.dca.archunit.fixtures.naming.trigger.bad.archive.application.restoreentry;

public final class RestoreEntryAfterLoginUseCase implements RestoreEntryAfterLoginInputPort {
  @Override
  public RestoreEntryAfterLoginResult execute(RestoreEntryAfterLoginCommand input) {
    return new RestoreEntryAfterLoginResult(input.reference());
  }
}
