package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.model;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.good.premiums.domain.gateway.RiskRates;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.Value;

/** Calls a gateway it receives as a parameter. */
public record Premium(int perMille) implements Value {
  public static Premium in(Region region, RiskRates rates) {
    return new Premium(rates.rateFor(region).perMille());
  }
}
