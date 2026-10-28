package dev.domaincentric.dca.archunit.fixtures.frameworks.jakarta.billing.adapter.outgoing;

import jakarta.ws.rs.Path;

/** A declarative REST client of a partner system: an interface carrying the JAX-RS path. */
@Path("/tax-rates")
public interface TaxServiceClient {
  String rateFor(String country);
}
