package dev.domaincentric.dca.archunit.fixtures.ownmarkersclean.billing.domain.model;

import dev.domaincentric.dca.archunit.fixtures.ownmarkersclean.vocabulary.ContractRoot;

/** An aggregate that depends on nothing but its own vocabulary and the platform. */
public final class TariffContract implements ContractRoot {

  private final String tariff;

  public TariffContract(String tariff) {
    this.tariff = tariff;
  }

  public String tariff() {
    return tariff;
  }
}
