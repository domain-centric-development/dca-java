package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.adapter.incoming;

import dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder.AuditStore;

/** DCA-TAC-020: store implementation outside adapter.outgoing. */
public class InMemoryAuditStore implements AuditStore {
  @Override
  public void save(String entry) {}

  @Override
  public java.util.concurrent.CompletableFuture<Void> saveAsync(String entry) {
    return java.util.concurrent.CompletableFuture.completedFuture(null);
  }
}
