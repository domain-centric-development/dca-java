package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway.RiskRates;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

/** Holds the gateway in a field. */
public final class Policy extends BaseAggregateRoot<Policy, PolicyId> {
  private final PolicyId id;
  private final Region region;
  private final RiskRates rates;
  private int premium;

  private Policy(PolicyId id, Region region, RiskRates rates) {
    this.id = id;
    this.region = region;
    this.rates = rates;
  }

  public static Policy open(PolicyId id, Region region, RiskRates rates) {
    return new Policy(id, region, rates);
  }

  public void price() {
    this.premium = rates.rateFor(region).perMille();
  }

  @Override
  public PolicyId id() {
    return id;
  }

  public int premium() {
    return premium;
  }
}
