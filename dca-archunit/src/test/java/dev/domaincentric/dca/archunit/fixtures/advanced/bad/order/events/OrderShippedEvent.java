package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.events;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEventType;
import java.time.Instant;
import java.util.UUID;

@IntegrationEventType(name = "order.shipped", version = 2)
public record OrderShippedEvent(UUID eventId, Instant occurredOn, int schemaVersion)
    implements IntegrationEvent {}
