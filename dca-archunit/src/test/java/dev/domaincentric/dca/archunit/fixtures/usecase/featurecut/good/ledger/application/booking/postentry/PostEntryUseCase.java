package dev.domaincentric.dca.archunit.fixtures.usecase.featurecut.good.ledger.application.booking.postentry;

public final class PostEntryUseCase implements PostEntryInputPort {
  @Override
  public PostEntryResult execute(PostEntryCommand input) {
    return new PostEntryResult(input.reference());
  }
}
