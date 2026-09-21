package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;

/**
 * DCA-TAC-021: repository semantics on a Store. A use-case-local output port is legal, so TAC-019
 * passes.
 */
public interface AuditStore extends Store {
  void save(String entry);

  /** The asynchronous spelling is the same name to DCA-TAC-021, in both languages. */
  java.util.concurrent.CompletableFuture<Void> saveAsync(String entry);
}
