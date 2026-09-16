package dev.domaincentric.dca.archunit.fixtures.layout.isolation.reporting.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.domain.model.Product;

/**
 * Forbidden: an incoming adapter of an <em>undeclared</em> module reaching into another module's
 * internals (DCA-HEX-007). Structural selection makes the undeclared module a subject here too.
 */
public class ReportController {
  public String report(final Product product) {
    return product.sku();
  }
}
