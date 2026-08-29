package dev.domaincentric.dca.archunit.fixtures.advanced.bad.order.application.misplaced;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record OrderMisplaced(UUID eventId, Instant occurredOn) implements DomainEvent {}
