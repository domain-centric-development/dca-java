package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway.GatewayRegistry;
import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway.RiskRates;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** Holds the gateway in a static field, filled from another type's static field. */
public record Premium(int perMille) implements Value {
  private static final RiskRates RATES = GatewayRegistry.RATES;

  public static Premium in(Region region) {
    return new Premium(RATES.rateFor(region).perMille());
  }
}
