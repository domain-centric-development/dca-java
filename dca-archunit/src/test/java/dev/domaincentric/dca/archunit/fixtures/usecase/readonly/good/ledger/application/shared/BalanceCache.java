package dev.domaincentric.dca.archunit.fixtures.usecase.readonly.good.ledger.application.shared;

import dev.domaincentric.dca.buildingblocks.hexagonal.port.out.Store;
import java.util.Optional;

/** Data no aggregate owns: a read and a write. */
public interface BalanceCache extends Store {
  Optional<Long> lookup(String account);

  void putBalance(String account, long balance);
}
