package dev.domaincentric.dca.archunit.fixtures.contextmap.good.catalog.api;

import dev.domaincentric.dca.buildingblocks.ddd.strategic.relationships.OpenHostService;

@OpenHostService(context = "catalog")
public interface CatalogApi {
  ProductInfo product(String id);
}
