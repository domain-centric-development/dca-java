package dev.domaincentric.dca.archunit.fixtures.hexagonal.listeners.good.ledger.domain.model;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record EntryPosted(UUID eventId, String reference, Instant occurredOn)
    implements DomainEvent {}
