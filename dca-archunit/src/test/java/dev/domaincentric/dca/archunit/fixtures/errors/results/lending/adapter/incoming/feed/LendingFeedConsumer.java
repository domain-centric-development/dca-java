package dev.domaincentric.dca.archunit.fixtures.errors.results.lending.adapter.incoming.feed;

import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy.LendCopyCommand;
import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy.LendCopyInputPort;
import dev.domaincentric.dca.archunit.fixtures.errors.results.lending.application.lendcopy.LendCopyResult;

/** Receives a closed result and looks at none of its cases - a refusal is dropped. */
public final class LendingFeedConsumer {
  private final LendCopyInputPort lendCopy;

  public LendingFeedConsumer(LendCopyInputPort lendCopy) {
    this.lendCopy = lendCopy;
  }

  public String on(String memberId, String copyId) {
    LendCopyResult ignored = lendCopy.execute(new LendCopyCommand(memberId, copyId));
    return String.valueOf(ignored);
  }
}
