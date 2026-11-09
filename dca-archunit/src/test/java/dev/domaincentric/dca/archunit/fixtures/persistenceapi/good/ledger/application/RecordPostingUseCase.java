package dev.domaincentric.dca.archunit.fixtures.persistenceapi.good.ledger.application;

/** Talks to its output port only; the boundary is drawn around it. */
public class RecordPostingUseCase {

  private final Postings postings;

  public RecordPostingUseCase(Postings postings) {
    this.postings = postings;
  }

  public void execute(String posting) {
    postings.add(posting);
  }
}
