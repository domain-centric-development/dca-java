package dev.domaincentric.dca.archunit.fixtures.layout.isolation.reporting.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.api.CatalogService;

/**
 * Allowed: an incoming adapter depending on another module's published api package - the same
 * allow-list DCA-STR-006 grants outgoing adapters (DCA-HEX-007).
 */
public class ReportApiController {
  public String report(final CatalogService catalog) {
    return catalog.describe("sku");
  }
}
