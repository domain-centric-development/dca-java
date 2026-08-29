package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.events;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(UUID eventId, Instant occurredOn) implements IntegrationEvent {}
