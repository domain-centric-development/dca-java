package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway.FixedRiskRates;
import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway.RiskRates;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Specification;

/** Builds a gateway implementation and hands it out. */
public final class InsurableRegion implements Specification<Region> {
  public RiskRates rates() {
    return new FixedRiskRates();
  }

  @Override
  public boolean isSatisfiedBy(Region candidate) {
    return rates().rateFor(candidate).perMille() < 100;
  }
}
