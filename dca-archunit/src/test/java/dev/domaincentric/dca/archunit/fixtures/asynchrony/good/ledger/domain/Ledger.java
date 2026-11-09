package dev.domaincentric.dca.archunit.fixtures.asynchrony.good.ledger.domain;

/** A synchronous domain model. */
public class Ledger {

  private int balance;

  public int balance() {
    return balance;
  }

  public void post(int amount) {
    balance += amount;
  }
}
