package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.application.usecase;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.gateway.RiskRates;
import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.model.Policy;
import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.model.PolicyId;
import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.model.Region;

/** The application calls the gateway too. */
public final class QuotePremiumService {
  private final RiskRates rates;

  public QuotePremiumService(RiskRates rates) {
    this.rates = rates;
  }

  public int quote(String region) {
    Policy policy = Policy.open(new PolicyId("p-1"), new Region(region));
    policy.price(rates.rateFor(policy.region()));
    return policy.premium();
  }
}
