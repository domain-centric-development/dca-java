package dev.domaincentric.dca.archunit.fixtures.eventpolicy.module.events;

@dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEventType(
    name = "exported",
    version = 1)
public record Exported(java.util.UUID eventId, java.time.Instant occurredOn, int version)
    implements dev.domaincentric.dca.buildingblocks.ddd.tactical.IntegrationEvent {}
