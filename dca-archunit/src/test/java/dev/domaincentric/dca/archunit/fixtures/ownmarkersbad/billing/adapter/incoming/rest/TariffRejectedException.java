package dev.domaincentric.dca.archunit.fixtures.ownmarkersbad.billing.adapter.incoming.rest;

import dev.domaincentric.dca.archunit.fixtures.ownmarkersbad.vocabulary.ContractFailure;

/**
 * DCA-ERR-002: a failure of the model declared in an incoming adapter. Reached only when the role's
 * marker is matched through a base class.
 */
public final class TariffRejectedException extends ContractFailure {

  public TariffRejectedException(String tariff) {
    super("tariff " + tariff + " was rejected");
  }
}
