package dev.domaincentric.dca.archunit.fixtures.layout.isolation.peer.domain.model;

import dev.domaincentric.dca.archunit.fixtures.layout.isolation.catalog.api.CatalogService;

/**
 * Forbidden: a domain layer may not depend on another module at all, not even its api
 * (DCA-STR-004).
 */
public record PeerListing(CatalogService catalog) {}
