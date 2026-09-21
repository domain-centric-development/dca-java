package dev.domaincentric.dca.archunit.fixtures.ownmarkers.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.ownmarkers.vocabulary.ContractFailure;

/** A domain failure in its own layer — nothing for DCA-ERR-002 to report. */
public final class TariffAlreadyBilledException extends ContractFailure {

  public TariffAlreadyBilledException(String tariff) {
    super("tariff " + tariff + " is already billed");
  }
}
