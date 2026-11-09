package dev.domaincentric.dca.archunit.fixtures.persistenceapi.good.ledger.application;

import jakarta.transaction.Transactional;

/** Declares its transaction with the configured transactional annotation. Passes. */
public class CloseDayUseCase {

  private final Postings postings;

  public CloseDayUseCase(Postings postings) {
    this.postings = postings;
  }

  @Transactional
  public void execute() {
    postings.add("day closed");
  }
}
