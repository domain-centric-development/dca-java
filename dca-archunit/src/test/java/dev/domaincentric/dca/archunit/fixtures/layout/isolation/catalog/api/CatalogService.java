package dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.api;

/**
 * The published contract — the api package is where public sources live (Spring Modulith named
 * interface).
 */
public interface CatalogService {
  String describe(String sku);
}
