package dev.domaincentric.dca.archunit.fixtures.asynchrony.bad.ledger.domain;

/** Uses the code base's own awaitable: not reported unless the layout names it. */
public class Posting {

  public Deferred<Integer> amountLater() {
    return new Deferred<>(0);
  }
}
