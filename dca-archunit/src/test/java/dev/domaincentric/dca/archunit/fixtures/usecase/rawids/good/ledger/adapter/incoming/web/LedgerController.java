package dev.domaincentric.dca.archunit.fixtures.usecase.rawids.good.ledger.adapter.incoming.web;

import java.util.UUID;

/** The adapter parses at the edge and answers malformed text itself. */
public final class LedgerController {
  public boolean wellFormed(String text) {
    try {
      UUID.fromString(text);
      return true;
    } catch (IllegalArgumentException malformed) {
      return false;
    }
  }
}
