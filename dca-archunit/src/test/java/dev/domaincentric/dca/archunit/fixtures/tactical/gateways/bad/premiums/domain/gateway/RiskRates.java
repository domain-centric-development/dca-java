package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway;

import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model.Region;
import dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.model.RiskRate;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainGateway;

public interface RiskRates extends DomainGateway {
  RiskRate rateFor(Region region);
}
