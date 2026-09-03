package dev.domaincentric.dca.archunit.fixtures.strategic.good.catalog.adapter.incoming.rest;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.OpenHostService;

/**
 * An Open Host Service over the network: a REST resource. Any sub-package of the incoming adapter
 * is fine — the pattern is about what is published, not about the transport or the folder.
 */
@OpenHostService(context = "Catalog", description = "REST protocol for other contexts")
public class CatalogResource {
  public String describe(final String sku) {
    return sku;
  }
}
