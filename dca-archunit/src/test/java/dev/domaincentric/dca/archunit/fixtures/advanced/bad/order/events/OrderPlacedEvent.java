package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.events;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEventType;
import java.time.Instant;
import java.util.UUID;

@IntegrationEventType(name = "order.placed", version = 1)
public record OrderPlacedEvent(UUID eventId, Instant occurredOn, String orderId)
    implements IntegrationEvent {}
