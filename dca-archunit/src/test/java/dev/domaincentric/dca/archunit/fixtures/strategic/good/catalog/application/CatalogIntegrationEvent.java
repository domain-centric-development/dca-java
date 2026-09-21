package dev.domaincentric.dca.archunit.fixtures.strategic.good.catalog.application;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;

/**
 * A published-language contract the context's integration events share, declared outside the events
 * package on purpose. DCA-STR-007 must not report it for its package, and DCA-STR-008 must not ask
 * an interface to be final with final instance fields.
 */
public interface CatalogIntegrationEvent extends IntegrationEvent {}
