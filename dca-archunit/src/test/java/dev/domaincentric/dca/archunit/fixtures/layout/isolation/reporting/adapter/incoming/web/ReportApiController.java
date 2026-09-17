package dev.domaincentric.dca.archunit.fixtures.layout.isolation.reporting.adapter.incoming.web;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.api.CatalogService;

/**
 * Forbidden too: an incoming adapter depending on another module's <em>published</em> api package
 * (DCA-HEX-007). The controller reaches a sibling through its own use case, output port and
 * outgoing adapter; only DCA-STR-006 opens the api to outgoing adapters.
 */
public class ReportApiController {
  public String report(final CatalogService catalog) {
    return catalog.describe("sku");
  }
}
