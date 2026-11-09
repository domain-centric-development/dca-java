package dev.domaincentric.dca.archunit.fixtures.asynchrony.bad.ledger.domain;

import java.util.concurrent.Flow;

/** Holds a publisher - a type nested in a configured asynchronous type. Reported. */
public class PostingFeed {

  private Flow.Publisher<String> postings;

  public Flow.Publisher<String> postings() {
    return postings;
  }
}
