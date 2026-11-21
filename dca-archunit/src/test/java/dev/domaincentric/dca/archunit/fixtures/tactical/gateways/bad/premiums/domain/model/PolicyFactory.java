package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway.GatewayRegistry;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Factory;

/** Looks the gateway up from a registry. */
public final class PolicyFactory implements Factory {
  public Premium quote(Region region) {
    return new Premium(GatewayRegistry.riskRates().rateFor(region).perMille());
  }
}
