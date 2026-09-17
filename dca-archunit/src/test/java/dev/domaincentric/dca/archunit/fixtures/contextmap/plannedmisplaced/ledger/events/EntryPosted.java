package dev.domaincentric.dca.archunit.fixtures.contextmap.plannedmisplaced.ledger.events;

import dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent;
import java.time.Instant;
import java.util.UUID;

public record EntryPosted(UUID eventId, Instant occurredOn, String account)
    implements IntegrationEvent {}
