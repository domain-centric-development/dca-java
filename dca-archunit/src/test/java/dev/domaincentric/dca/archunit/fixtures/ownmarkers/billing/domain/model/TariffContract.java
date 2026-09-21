package dev.domaincentric.dca.archunit.fixtures.ownmarkers.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.ownmarkers.billing.application.shared.TariffContractStore;
import dev.domaincentric.dca.archunit.fixtures.ownmarkers.vocabulary.ContractRoot;

/**
 * DCA-TAC-002: an aggregate holding its own repository — visible only once the roles point here.
 */
public final class TariffContract implements ContractRoot {

  private final TariffContractStore contracts;

  public TariffContract(TariffContractStore contracts) {
    this.contracts = contracts;
  }

  public boolean knowsItsStore() {
    return contracts != null;
  }
}
