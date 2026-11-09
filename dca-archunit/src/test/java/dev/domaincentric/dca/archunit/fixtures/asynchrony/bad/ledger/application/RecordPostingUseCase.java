package dev.domaincentric.dca.archunit.fixtures.asynchrony.bad.ledger.application;

import java.util.concurrent.CompletableFuture;

/** Asynchrony in the application layer is not this rule's subject. Not reported. */
public class RecordPostingUseCase {

  public CompletableFuture<Void> execute(String posting) {
    return CompletableFuture.completedFuture(null);
  }
}
