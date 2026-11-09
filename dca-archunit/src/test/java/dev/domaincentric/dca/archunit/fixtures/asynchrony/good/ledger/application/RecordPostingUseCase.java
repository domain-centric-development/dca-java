package dev.domaincentric.dca.archunit.fixtures.asynchrony.good.ledger.application;

import dev.domaincentric.dca.archunit.fixtures.asynchrony.good.ledger.domain.Ledger;
import java.util.concurrent.CompletableFuture;

/** Awaits in the application layer and passes plain values into the domain. */
public class RecordPostingUseCase {

  private final Ledger ledger = new Ledger();

  public CompletableFuture<Integer> execute(int amount) {
    ledger.post(amount);
    return CompletableFuture.completedFuture(ledger.balance());
  }
}
