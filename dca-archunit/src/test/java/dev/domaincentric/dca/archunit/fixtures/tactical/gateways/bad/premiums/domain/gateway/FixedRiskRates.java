package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model.Region;
import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model.RiskRate;

/** An implementation of the gateway, which the model must not build. */
public final class FixedRiskRates implements RiskRates {
  @Override
  public RiskRate rateFor(Region region) {
    return new RiskRate(10);
  }
}
