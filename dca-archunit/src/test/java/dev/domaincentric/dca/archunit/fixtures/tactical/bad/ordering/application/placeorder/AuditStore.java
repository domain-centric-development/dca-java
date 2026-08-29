package dev.domaincentric.dca.archunit.fixtures.tactical.bad.ordering.application.placeorder;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;

/** DCA-TAC-019 (not in application.shared), DCA-TAC-021 (repository semantics). */
public interface AuditStore extends Store {
  void save(String entry);
}
