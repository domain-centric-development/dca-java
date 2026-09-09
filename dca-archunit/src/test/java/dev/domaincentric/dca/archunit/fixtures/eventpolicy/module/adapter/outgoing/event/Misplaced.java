package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.adapter.outgoing.event;

public record Misplaced(java.util.UUID eventId, java.time.Instant occurredOn)
    implements dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent {}
