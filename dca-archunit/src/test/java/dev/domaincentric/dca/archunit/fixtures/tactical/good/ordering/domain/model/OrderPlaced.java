package dev.domaincentric.dca.archunit.fixtures.tactical.good.ordering.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record OrderPlaced(UUID eventId, Instant occurredOn, OrderId orderId)
    implements DomainEvent {}
