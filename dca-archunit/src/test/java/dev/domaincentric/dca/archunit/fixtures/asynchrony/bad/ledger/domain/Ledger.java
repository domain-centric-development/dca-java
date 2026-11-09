package dev.domaincentric.dca.archunit.fixtures.asynchrony.bad.ledger.domain;

import java.util.concurrent.CompletableFuture;

/** Hands back a future: the domain has become asynchronous. Reported. */
public class Ledger {

  private int balance;

  public CompletableFuture<Integer> balanceLater() {
    return CompletableFuture.completedFuture(balance);
  }
}
