package dev.domaincentric.dca.archunit.fixtures.layout.isolation.reporting.application.getreport;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.domain.model.Product;

/**
 * The measurement. Same violation as the control group, but this module declares no
 * {@code @BoundedContext} — so it is never a *source* in the strategic rules.
 */
public class GetReportUseCase {
  public String describe(final Product product) {
    return product.sku();
  }
}
