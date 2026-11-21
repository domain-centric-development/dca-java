package dev.domaincentric.dca.archunit.fixtures.tactical.gateways.bad.premiums.domain.gateway;

/** A service locator: the form the model must not ask. */
public final class GatewayRegistry {
  public static final RiskRates RATES = new FixedRiskRates();

  private GatewayRegistry() {}

  public static RiskRates riskRates() {
    return RATES;
  }
}
