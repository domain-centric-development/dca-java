package dev.domaincentric.dca.archunit.fixtures.strategic.bad.catalog.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

public record ProductAddedEvent(UUID eventId, Instant occurredOn) implements IntegrationEvent {}
