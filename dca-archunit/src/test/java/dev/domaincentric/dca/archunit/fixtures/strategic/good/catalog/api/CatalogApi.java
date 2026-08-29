package dev.domaincentric.dca.archunit.fixtures.strategic.good.catalog.api;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.OpenHostService;

@OpenHostService(context = "catalog")
public interface CatalogApi {
  String productName(String productId);
}
