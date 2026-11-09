package dev.domaincentric.dca.archunit.fixtures.persistenceapi.good.ledger.application;

/** The output port the use case programs against. */
public interface Postings {

  void add(String posting);
}
