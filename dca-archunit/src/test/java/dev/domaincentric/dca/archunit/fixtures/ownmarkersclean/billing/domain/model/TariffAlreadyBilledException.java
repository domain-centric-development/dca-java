package dev.domaincentric.dca.archunit.fixtures.ownmarkersclean.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.ownmarkersclean.vocabulary.ContractFailure;

/** A domain failure in its own layer, carrying the code base's own base type. */
public final class TariffAlreadyBilledException extends ContractFailure {

  public TariffAlreadyBilledException(String tariff) {
    super("tariff " + tariff + " is already billed");
  }
}
