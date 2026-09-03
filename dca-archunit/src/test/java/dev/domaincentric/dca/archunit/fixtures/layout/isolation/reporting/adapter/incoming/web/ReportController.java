package dev.domaincentric.dca.archunit.fixtures.layout.isolation.reporting.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.api.CatalogService;

/**
 * Forbidden: an incoming adapter of an <em>undeclared</em> module orchestrating another module
 * (DCA-HEX-007). Structural selection makes the undeclared module a subject here too.
 */
public class ReportController {
  public String report(final CatalogService catalog) {
    return catalog.describe("sku");
  }
}
