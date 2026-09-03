package dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.domain.model;

/** Internal. Nobody outside the catalog context may touch this. */
public record Product(String sku) {}
