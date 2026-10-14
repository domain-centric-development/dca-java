package dev.domaincentric.dca.archunit.fixtures.tactical.factories.good.ledger.domain.model;

public interface Hasher {
  String hash(String plaintext);
}
