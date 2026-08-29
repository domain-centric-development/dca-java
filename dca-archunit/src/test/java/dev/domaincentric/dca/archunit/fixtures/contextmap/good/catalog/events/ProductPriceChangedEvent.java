package dev.domaincentric.dca.archunit.fixtures.contextmap.good.catalog.events;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

public record ProductPriceChangedEvent(UUID eventId, Instant occurredOn, String productId)
    implements IntegrationEvent {}
