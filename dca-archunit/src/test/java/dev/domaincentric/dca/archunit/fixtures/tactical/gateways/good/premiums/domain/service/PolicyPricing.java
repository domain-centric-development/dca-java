package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.service;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.gateway.RiskRates;
import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.model.Policy;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainService;

/** A domain service calls the gateway and hands the fact to the aggregate. */
public final class PolicyPricing implements DomainService {
  private final RiskRates rates;

  public PolicyPricing(RiskRates rates) {
    this.rates = rates;
  }

  public void price(Policy policy) {
    policy.price(rates.rateFor(policy.region()));
  }
}
