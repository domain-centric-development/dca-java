package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.gateway.RiskRates;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.BaseAggregateRoot;

/** Takes the fact, or the gateway itself as a parameter. */
public final class Policy extends BaseAggregateRoot<Policy, PolicyId> {
  private final PolicyId id;
  private final Region region;
  private int premium;

  private Policy(PolicyId id, Region region) {
    this.id = id;
    this.region = region;
  }

  public static Policy open(PolicyId id, Region region) {
    return new Policy(id, region);
  }

  public void price(RiskRate rate) {
    this.premium = rate.perMille();
  }

  public void reprice(RiskRates rates) {
    this.premium = Premium.in(region, rates).perMille();
  }

  @Override
  public PolicyId id() {
    return id;
  }

  public Region region() {
    return region;
  }

  public int premium() {
    return premium;
  }
}
