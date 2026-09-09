package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.domain.model;

public record Changed(java.util.UUID eventId, java.time.Instant occurredOn, int version)
    implements dev.domaincentric.dca.buildingblocks.ddd.tactical.DomainEvent {}
