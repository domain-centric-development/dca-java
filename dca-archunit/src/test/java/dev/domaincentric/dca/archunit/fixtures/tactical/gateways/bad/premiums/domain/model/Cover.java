package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway.RiskRates;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Entity;
import java.util.List;

/** Holds the gateway, hidden in a type argument. */
public final class Cover implements Entity<Cover, PolicyId> {
  private final PolicyId id;
  private final List<RiskRates> sources;

  private Cover(PolicyId id, List<RiskRates> sources) {
    this.id = id;
    this.sources = sources;
  }

  @Override
  public PolicyId id() {
    return id;
  }

  public int sourceCount() {
    return sources.size();
  }
}
