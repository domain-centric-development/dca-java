package dev.domaincentric.dca.archunit.fixtures.hexagonal.getmapping.bad.ledger.application.postentry;

public final class PostEntryUseCase implements PostEntryInputPort {
  @Override
  public PostEntryResult execute(PostEntryCommand input) {
    return new PostEntryResult(input.reference());
  }
}
